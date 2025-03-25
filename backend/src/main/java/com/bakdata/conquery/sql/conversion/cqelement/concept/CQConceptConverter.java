package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.ConqueryJoinType;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionType;
import com.bakdata.conquery.sql.conversion.model.filter.ConditionUtil;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ConceptSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.util.TablePrimaryColumnUtil;
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

	private static QueryStep finishConceptConversion(QueryStep predecessor, CQConcept cqConcept, TablePath tablePath, ConversionContext context) {

		ConceptSqlTables universalTables = tablePath.createConceptTables(predecessor);

		Selects predecessorSelects = predecessor.getQualifiedSelects();
		Optional<ColumnDateRange> validityDate = predecessorSelects.getValidityDate();
		SqlIdColumns ids = predecessorSelects.getIds();

		SelectContext<ConceptSqlTables> selectContext = SelectContext.create(ids, validityDate, universalTables, context);
		List<ConceptSqlSelects> converted = cqConcept.getSelects().stream()
													 .map(selectId -> {
														 Select select = selectId.resolve();
														 return select.createConverter().conceptSelect(select, selectContext);
													 })
													 .toList();

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(predecessor);
		converted.stream().map(ConceptSqlSelects::getAdditionalPredecessor).filter(Optional::isPresent).map(Optional::get).forEach(queriesToJoin::add);

		if (universalTables.isRequiredStep(ConceptCteStep.INTERVAL_PACKING_SELECTS)) {
			QueryStep eventDateSelectsStep = IntervalPackingSelectsCte.forConcept(predecessor, universalTables, converted, context);
			queriesToJoin.add(eventDateSelectsStep);
		}

		// combine all universal selects and connector selects from preceding step
		List<SqlSelect> allConceptSelects = Stream.concat(
														  converted.stream().flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream()),
														  predecessor.getQualifiedSelects().getSqlSelects().stream().map(SqlSelect::connectorAggregate)
												  )
												  .toList();

		Selects finalSelects = Selects.builder()
									  .ids(ids)
									  .stratificationDate(predecessorSelects.getStratificationDate())
									  .validityDate(validityDate)
									  .sqlSelects(allConceptSelects)
									  .build();

		TableLike<Record> joinedTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, ConqueryJoinType.INNER_JOIN, context);

		List<Field<?>> groupByFields =
				Stream.concat(
						finalSelects.getIds().toFields().stream(),
						predecessor.getQualifiedSelects().getSqlSelects().stream().filter(Predicate.not(SqlSelect::isUniversal))
								.flatMap(sqlSelect -> sqlSelect.toFields().stream())
				).toList();

		return QueryStep.builder()
						.cteName(universalTables.cteName(ConceptCteStep.UNIVERSAL_SELECTS))
						.selects(finalSelects)
						.fromTable(joinedTable)
					    .groupBy(groupByFields)
						.predecessors(queriesToJoin)
						.build();
	}

	public static SqlIdColumns convertIds(CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		Table table = cqTable.getConnector().resolve().getResolvedTable();
		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(table, conversionContext.getConfig());

		if (cqConcept.isExcludeFromSecondaryId()
			|| conversionContext.getSecondaryIdDescription() == null
			|| !cqTable.hasSelectedSecondaryId(conversionContext.getSecondaryIdDescription())
		) {
			return new SqlIdColumns(primaryColumn).withAlias();
		}

		Column secondaryIdColumn = table.findSecondaryIdColumn(conversionContext.getSecondaryIdDescription().getId());

		Preconditions.checkArgument(
				secondaryIdColumn != null,
				"Expecting Table %s to have a matching secondary id for %s".formatted(
						table,
						conversionContext.getSecondaryIdDescription()
				)
		);

		Field<Object> secondaryId = DSL.field(DSL.name(table.getName(), secondaryIdColumn.getName()));
		return new SqlIdColumns(primaryColumn, secondaryId).withAlias();
	}

	private static Optional<ColumnDateRange> convertValidityDate(CQTable cqTable, String connectorLabel, ConversionContext context) {
		if (Objects.isNull(cqTable.findValidityDate())) {
			return Optional.empty();
		}
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		ColumnDateRange validityDate;
		if (context.getDateRestrictionRange() != null) {
			validityDate = functionProvider.forValidityDate(cqTable.findValidityDate(), context.getDateRestrictionRange()).asValidityDateRange(connectorLabel);
		}
		else {
			validityDate = functionProvider.forValidityDate(cqTable.findValidityDate()).asValidityDateRange(connectorLabel);
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
						ConnectorSqlSelects.none(),
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
		WhereCondition childCondition = child.getCondition().convertToSqlCondition(CTConditionContext.create(cqTable.getConnector().resolve(), functionProvider));
		return Stream.concat(
				collectConditions(cqTable, child.getParent(), functionProvider),
				Stream.of(childCondition)
		);
	}

	private static Optional<WhereCondition> convertConnectorCondition(CQTable cqTable, SqlFunctionProvider functionProvider) {
		final Connector connector = cqTable.getConnector().resolve();

		return Optional.ofNullable(connector.getCondition())
					   .map(condition -> condition.convertToSqlCondition(CTConditionContext.create(connector, functionProvider)));
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
				ConnectorSqlSelects.builder().preprocessingSelects(dateRestrictionSelects).build(),
				WhereClauses.builder().eventFilter(ConditionUtil.wrap(dateRestrictionCondition, ConditionType.EVENT)).build()
		));
	}

	private static ConnectorSqlSelects createConceptColumnConnectorSqlSelects(CQConcept cqConcept, SelectContext<ConnectorSqlTables> selectContext) {
		return cqConcept.getSelects().stream()
						.map(SelectId::resolve)
						.filter(select -> select instanceof ConceptColumnSelect)
						.findFirst()
						.map(select -> select.createConverter().connectorSelect(select, selectContext))
						.orElse(ConnectorSqlSelects.none());
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept cqConcept, ConversionContext context) {

		TablePath tablePath = new TablePath(cqConcept, context);
		List<QueryStep> convertedCQTables = cqConcept.getTables().stream()
													 .flatMap(cqTable -> convertCqTable(tablePath, cqConcept, cqTable, context).stream())
													 .toList();

		QueryStep joinedStep = QueryStepJoiner.joinSteps(convertedCQTables, ConqueryJoinType.OUTER_JOIN, DateAggregationAction.MERGE, context);
		QueryStep lastConceptStep = finishConceptConversion(joinedStep, cqConcept, tablePath, context);
		return context.withQueryStep(lastConceptStep);
	}

	private Optional<QueryStep> convertCqTable(TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext context) {
		CQTableContext tableContext = createTableContext(tablePath, cqConcept, cqTable, context);
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

	private CQTableContext createTableContext(TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		SqlIdColumns ids = convertIds(cqConcept, cqTable, conversionContext);
		ConnectorSqlTables connectorTables = tablePath.getConnectorTables(cqTable);
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(cqTable, connectorTables.getLabel(), conversionContext);

		// convert filters
		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();
		cqTable.getFilters().stream()
			   .map(filterValue -> filterValue.convertToSqlFilter(ids, conversionContext, connectorTables))
			   .forEach(allSqlFiltersForTable::add);
		collectConditionFilters(cqConcept.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList(), cqTable, functionProvider).ifPresent(allSqlFiltersForTable::add);
		getDateRestriction(conversionContext, tablesValidityDate).ifPresent(allSqlFiltersForTable::add);

		// convert selects
		SelectContext<ConnectorSqlTables> selectContext = SelectContext.create(ids, tablesValidityDate, connectorTables, conversionContext);
		List<ConnectorSqlSelects> allSelectsForTable = new ArrayList<>();
		ConnectorSqlSelects conceptColumnSelect = createConceptColumnConnectorSqlSelects(cqConcept, selectContext);
		allSelectsForTable.add(conceptColumnSelect);
		cqTable.getSelects().stream().map(SelectId::resolve).map(select -> select.createConverter().connectorSelect(select, selectContext)).forEach(allSelectsForTable::add);

		return CQTableContext.builder()
							 .ids(ids)
							 .validityDate(tablesValidityDate)
							 .sqlSelects(allSelectsForTable)
							 .sqlFilters(allSqlFiltersForTable)
							 .connectorTables(connectorTables)
							 .conversionContext(conversionContext)
							 .build();
	}

}
