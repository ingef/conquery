package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

	@Getter
	@RequiredArgsConstructor
	private enum ConceptCteStep implements CteStep {
		UNIVERSAL_SELECTS("universal_selects");
		private final String suffix;
	}

	private final List<ConnectorCte> connectorCTEs;

	public CQConceptConverter() {
		this.connectorCTEs = List.of(
				new PreprocessingCte(),
				new EventFilterCte(),
				new AggregationSelectCte(),
				new JoinBranchesCte(),
				new AggregationFilterCte()
		);
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept cqConcept, ConversionContext context) {

		String label = context.getNameGenerator().conceptName(cqConcept);
		List<QueryStep> convertedConnectorTables = cqConcept.getTables().stream()
															.flatMap(cqTable -> convertCqTable(label, cqConcept, cqTable, context).stream())
															.toList();

		QueryStep lastConceptStep;
		if (convertedConnectorTables.size() == 1) {
			lastConceptStep = finishConceptConversion(label, convertedConnectorTables.get(0), cqConcept, context);
		}
		else {
			QueryStep joinedStep = QueryStepJoiner.joinSteps(convertedConnectorTables, LogicalOperation.OR, DateAggregationAction.MERGE, context);
			lastConceptStep = finishConceptConversion(label, joinedStep, cqConcept, context);
		}
		return context.withQueryStep(lastConceptStep);
	}

	private Optional<QueryStep> convertCqTable(String conceptLabel, CQConcept cqConcept, CQTable cqTable, ConversionContext context) {
		CQTableContext tableContext = createTableContext(conceptLabel, cqConcept, cqTable, context);
		Optional<QueryStep> lastQueryStep = Optional.empty();
		for (ConnectorCte queryStep : connectorCTEs) {
			Optional<QueryStep> convertedStep = queryStep.convert(tableContext, lastQueryStep);
			if (convertedStep.isEmpty()) {
				continue;
			}
			lastQueryStep = convertedStep;
			tableContext = tableContext.withPrevious(lastQueryStep.get());
		}
		return lastQueryStep;
	}

	private static QueryStep finishConceptConversion(String conceptLabel, QueryStep predecessor, CQConcept cqConcept, ConversionContext context) {

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		SelectContext selectContext = SelectContext.forUniversalSelects(predecessorSelects.getPrimaryColumn(), predecessorSelects.getValidityDate(), context);
		List<SqlSelect> universalSelects = cqConcept.getSelects().stream()
													.map(select -> select.convertToSqlSelects(selectContext))
													.flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
													.toList();

		List<SqlSelect> allConceptSelects = Stream.of(universalSelects, predecessorSelects.getSqlSelects())
												  .flatMap(List::stream)
												  .toList();

		Selects finalSelects = predecessorSelects.toBuilder()
												 .clearSqlSelects()
												 .sqlSelects(allConceptSelects)
												 .build();

		return QueryStep.builder()
						.cteName(context.getNameGenerator().cteStepName(ConceptCteStep.UNIVERSAL_SELECTS, conceptLabel))
						.selects(finalSelects)
						.fromTable(QueryStep.toTableLike(predecessor.getCteName()))
						.predecessors(List.of(predecessor))
						.build();
	}

	private CQTableContext createTableContext(String conceptLabel, CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		NameGenerator nameGenerator = conversionContext.getNameGenerator();
		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();

		Connector connector = cqTable.getConnector();
		String conceptConnectorLabel = nameGenerator.conceptConnectorName(cqConcept, connector);
		String tableName = connector.getTable().getName();

		Field<Object> primaryColumn = DSL.field(DSL.name(conversionContext.getConfig().getPrimaryColumn()));
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(cqTable, tableName, functionProvider);
		SqlTables connectorTables = ConnectorCteStep.createTables(conceptConnectorLabel, tableName, nameGenerator);

		// validity date
		IntervalPackingContext intervalPackingContext = null;
		if (intervalPackingRequired(tablesValidityDate, cqConcept)) {
			String preprocessingCteName = connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
			SqlTables intervalPackingTables = IntervalPackingCteStep.getTables(conceptConnectorLabel, preprocessingCteName, nameGenerator);
			intervalPackingContext = IntervalPackingContext.builder()
														   .nodeLabel(conceptConnectorLabel)
														   .primaryColumn(primaryColumn)
														   .validityDate(tablesValidityDate.get())
														   .intervalPackingTables(intervalPackingTables)
														   .build();
		}

		// convert filters
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();
		cqTable.getFilters().stream()
			   .map(filterValue -> filterValue.convertToSqlFilter(conversionContext, connectorTables))
			   .forEach(allSqlFiltersForTable::add);
		collectConditionFilters(cqConcept.getElements(), cqTable, functionProvider).ifPresent(allSqlFiltersForTable::add);
		getDateRestriction(conversionContext, tablesValidityDate).ifPresent(allSqlFiltersForTable::add);

		// convert selects
		SelectContext selectContext = SelectContext.forConnectorSelects(primaryColumn, tablesValidityDate, connectorTables, conversionContext);
		List<SqlSelects> allSelectsForTable = cqTable.getSelects().stream()
													 .map(select -> select.convertToSqlSelects(selectContext))
													 .toList();

		return CQTableContext.builder()
							 .conceptLabel(conceptLabel)
							 .conceptConnectorLabel(conceptConnectorLabel)
							 .primaryColumn(primaryColumn)
							 .validityDate(tablesValidityDate)
							 .sqlSelects(allSelectsForTable)
							 .sqlFilters(allSqlFiltersForTable)
							 .connectorTables(connectorTables)
							 .intervalPackingContext(intervalPackingContext)
							 .parentContext(conversionContext)
							 .build();
	}

	private static Optional<ColumnDateRange> convertValidityDate(CQTable cqTable, String label, SqlFunctionProvider functionProvider) {
		if (Objects.isNull(cqTable.findValidityDate())) {
			return Optional.empty();
		}
		ColumnDateRange validityDate = functionProvider.daterange(
				cqTable.findValidityDate(),
				cqTable.getConnector().getTable().getName(),
				label
		);
		return Optional.of(validityDate);
	}

	private static boolean intervalPackingRequired(Optional<ColumnDateRange> validityDate, CQConcept cqConcept) {
		return validityDate.isPresent() && !cqConcept.isExcludeFromTimeAggregation();
	}

	private static boolean dateRestrictionApplicable(boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		return dateRestrictionRequired && validityDateSelect.isPresent();
	}

	private static Optional<SqlFilters> collectConditionFilters(List<ConceptElement<?>> conceptElements, CQTable cqTable, SqlFunctionProvider functionProvider) {
		return collectConditions(conceptElements, cqTable, functionProvider)
				.stream()
				.reduce(WhereCondition::or)
				.map(whereCondition -> new SqlFilters(
						SqlSelects.builder().build(),
						WhereClauses.builder().preprocessingCondition(whereCondition).build()
				));
	}

	private static List<WhereCondition> collectConditions(List<ConceptElement<?>> conceptElements, CQTable cqTable, SqlFunctionProvider functionProvider) {

		List<WhereCondition> conditions = new ArrayList<>();
		convertConnectorCondition(cqTable, functionProvider).ifPresent(conditions::add);

		for (ConceptElement<?> conceptElement : conceptElements) {
			collectConditions(cqTable, (ConceptTreeNode<?>) conceptElement, functionProvider)
					.reduce(WhereCondition::and)
					.ifPresent(conditions::add);
		}

		return conditions;
	}

	/**
	 * Collects all conditions of a given {@link ConceptTreeNode} by resolving the condition of the given node and all of its parent nodes.
	 */
	private static Stream<WhereCondition> collectConditions(CQTable cqTable, ConceptTreeNode<?> conceptElement, SqlFunctionProvider functionProvider) {
		if (!(conceptElement instanceof ConceptTreeChild child)) {
			return Stream.empty();
		}
		WhereCondition childCondition = child.getCondition().convertToSqlCondition(CTConditionContext.create(cqTable.getConnector(), functionProvider));
		return Stream.concat(
				collectConditions(cqTable, child.getParent(), functionProvider),
				Stream.of(childCondition)
		);
	}

	private static Optional<WhereCondition> convertConnectorCondition(CQTable cqTable, SqlFunctionProvider functionProvider) {
		return Optional.ofNullable(cqTable.getConnector().getCondition())
					   .map(condition -> condition.convertToSqlCondition(CTConditionContext.create(cqTable.getConnector(), functionProvider)));
	}

	private static Optional<SqlFilters> getDateRestriction(ConversionContext context, Optional<ColumnDateRange> validityDate) {

		if (!dateRestrictionApplicable(context.dateRestrictionActive(), validityDate)) {
			return Optional.empty();
		}

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		ColumnDateRange dateRestriction = functionProvider.daterange(context.getDateRestrictionRange())
														  .asDateRestrictionRange();

		List<SqlSelect> dateRestrictionSelects = dateRestriction.toFields().stream()
																.map(FieldWrapper::new)
																.collect(Collectors.toList());

		Condition dateRestrictionCondition = functionProvider.dateRestriction(dateRestriction, validityDate.get());

		return Optional.of(new SqlFilters(
				SqlSelects.builder().preprocessingSelects(dateRestrictionSelects).build(),
				WhereClauses.builder().eventFilter(ConditionUtil.wrap(dateRestrictionCondition, ConditionType.EVENT)).build()
		));
	}

}
