package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

public class CQConceptConverter implements NodeConverter<CQConcept> {

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

		TablePathGenerator pathGenerator = new TablePathGenerator(context);
		List<QueryStep> convertedConnectorTables = cqConcept.getTables().stream()
															.flatMap(cqTable -> convertCqTable(pathGenerator, cqConcept, cqTable, context).stream())
															.toList();

		QueryStep lastConceptStep;
		if (convertedConnectorTables.size() == 1) {
			lastConceptStep = finishConceptConversion(convertedConnectorTables.get(0), cqConcept, pathGenerator, context);
		}
		else {
			QueryStep joinedStep = QueryStepJoiner.joinSteps(convertedConnectorTables, LogicalOperation.OUTER_JOIN, DateAggregationAction.MERGE, context);
			lastConceptStep = finishConceptConversion(joinedStep, cqConcept, pathGenerator, context);
		}
		return context.withQueryStep(lastConceptStep);
	}

	private Optional<QueryStep> convertCqTable(TablePathGenerator pathGenerator, CQConcept cqConcept, CQTable cqTable, ConversionContext context) {
		CQTableContext tableContext = createTableContext(pathGenerator, cqConcept, cqTable, context);
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

	private static QueryStep finishConceptConversion(QueryStep predecessor, CQConcept cqConcept, TablePathGenerator pathGenerator, ConversionContext context) {

		ConceptConversionTables universalTables = pathGenerator.createUniversalTables(predecessor, cqConcept);

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		SelectContext selectContext = new SelectContext(predecessorSelects.getIds(), predecessorSelects.getValidityDate(), universalTables, context);
		List<SqlSelects> converted = cqConcept.getSelects().stream()
											  .map(select -> select.convertToSqlSelects(selectContext))
											  .toList();

		List<QueryStep> queriesToJoin;
		if (universalTables.isRequiredStep(ConceptCteStep.INTERVAL_PACKING_SELECTS)) {
			QueryStep eventDateSelectsStep = IntervalPackingSelectsCte.forConcept(predecessor, universalTables, converted, context);
			queriesToJoin = List.of(predecessor, eventDateSelectsStep);
		}
		else {
			queriesToJoin = List.of(predecessor);
		}

		// combine all universal selects and connector selects from preceding step
		List<SqlSelect> allConceptSelects = Stream.concat(
														  converted.stream().flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream()),
														  predecessor.getQualifiedSelects().getSqlSelects().stream()
												  )
												  .toList();

		Selects finalSelects = Selects.builder()
									  .ids(predecessorSelects.getIds())
									  .stratificationDate(predecessorSelects.getStratificationDate())
									  .validityDate(predecessorSelects.getValidityDate())
									  .sqlSelects(allConceptSelects)
									  .build();

		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.INNER_JOIN, context);

		return QueryStep.builder()
						.cteName(universalTables.cteName(ConceptCteStep.UNIVERSAL_SELECTS))
						.selects(finalSelects)
						.fromTable(joinedTable)
						.predecessors(queriesToJoin)
						.build();
	}

	private CQTableContext createTableContext(TablePathGenerator pathGenerator, CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		NameGenerator nameGenerator = conversionContext.getNameGenerator();
		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();

		Connector connector = cqTable.getConnector();
		String conceptConnectorLabel = nameGenerator.conceptConnectorName(cqConcept, connector);

		SqlIdColumns ids = convertIds(cqConcept, cqTable, conversionContext);
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(cqTable, conceptConnectorLabel, conversionContext);
		ConceptConversionTables connectorTables = pathGenerator.createConnectorTables(cqConcept, cqTable, conceptConnectorLabel);

		// convert filters
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();
		cqTable.getFilters().stream()
			   .map(filterValue -> filterValue.convertToSqlFilter(ids, conversionContext, connectorTables))
			   .forEach(allSqlFiltersForTable::add);
		collectConditionFilters(cqConcept.getElements(), cqTable, functionProvider).ifPresent(allSqlFiltersForTable::add);
		getDateRestriction(conversionContext, tablesValidityDate).ifPresent(allSqlFiltersForTable::add);

		// convert selects
		SelectContext selectContext = new SelectContext(ids, tablesValidityDate, connectorTables, conversionContext);
		List<SqlSelects> allSelectsForTable = cqTable.getSelects().stream()
													 .map(select -> select.convertToSqlSelects(selectContext))
													 .toList();

		return CQTableContext.builder()
							 .ids(ids)
							 .validityDate(tablesValidityDate)
							 .sqlSelects(allSelectsForTable)
							 .sqlFilters(allSqlFiltersForTable)
							 .connectorTables(connectorTables)
							 .conversionContext(conversionContext)
							 .build();
	}

	private static SqlIdColumns convertIds(CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		Field<Object> primaryColumn = DSL.field(DSL.name(conversionContext.getConfig().getPrimaryColumn()));

		if (cqConcept.isExcludeFromSecondaryId()
			|| conversionContext.getSecondaryIdDescription() == null
			|| !cqTable.hasSelectedSecondaryId(conversionContext.getSecondaryIdDescription())
		) {
			return new SqlIdColumns(primaryColumn);
		}

		Column secondaryIdColumn = cqTable.getConnector().getTable().findSecondaryIdColumn(conversionContext.getSecondaryIdDescription());

		Preconditions.checkArgument(
				secondaryIdColumn != null,
				"Expecting Table %s to have a matching secondary id for %s".formatted(
						cqTable.getConnector().getTable(),
						conversionContext.getSecondaryIdDescription()
				)
		);

		Field<Object> secondaryId = DSL.field(DSL.name(secondaryIdColumn.getName()));
		return new SqlIdColumns(primaryColumn, secondaryId);
	}

	private static Optional<ColumnDateRange> convertValidityDate(CQTable cqTable, String connectorLabel, ConversionContext context) {
		if (Objects.isNull(cqTable.findValidityDate())) {
			return Optional.empty();
		}
		ColumnDateRange validityDate;
		if (context.getDateRestrictionRange() != null) {
			validityDate = context.getSqlDialect().getFunctionProvider().forTablesValidityDate(cqTable, context.getDateRestrictionRange(), connectorLabel);
		}
		else {
			validityDate = context.getSqlDialect().getFunctionProvider().forTablesValidityDate(cqTable, connectorLabel);
		}
		return Optional.of(validityDate);
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
		ColumnDateRange dateRestriction = functionProvider.forCDateRange(context.getDateRestrictionRange()).as(SharedAliases.DATE_RESTRICTION.getAlias());

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
