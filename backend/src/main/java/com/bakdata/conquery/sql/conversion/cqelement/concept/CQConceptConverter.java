package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.ConceptColumnSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectHavingConditionStep;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

@Slf4j
public class CQConceptConverter implements NodeConverter<CQConcept> {

	public static Field<Object> VALIDITY_DATE = field("validity_date");

	public CQConceptConverter() {
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
														  // aggregate special selects (e.g. Exists)
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

		// group by everything which is not part of an aggregation in this step
		//TODO use any_value or min instead of group-by

		return QueryStep.builder()
						.cteName(universalTables.cteName(ConceptCteStep.UNIVERSAL_SELECTS))
						.selects(finalSelects)
						.fromTable(joinedTable)
						.groupBy(finalSelects.nonExplicitSelects())
						.predecessors(queriesToJoin)
						.build();
	}

	public static SqlIdColumns convertIds(CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		Table table = cqTable.getConnector().resolve().getResolvedTable();
		Field<Object> primaryColumn = TablePrimaryColumnUtil.findPrimaryColumn(table, conversionContext.getConfig());

		if (cqConcept.isExcludeFromSecondaryId()
			|| conversionContext.getSecondaryIdDescription() == null
			|| !cqTable.hasSelectedSecondaryId(conversionContext.getSecondaryIdDescription().getId())
		) {
			return new SqlIdColumns(primaryColumn).withAlias();
		}

		Column secondaryIdColumn = table.findSecondaryIdColumn(conversionContext.getSecondaryIdDescription().getId());

		Field<Object> secondaryId = field(name(table.getName(), secondaryIdColumn.getName()));
		return new SqlIdColumns(primaryColumn, secondaryId).withAlias();
	}

	private static Optional<ColumnDateRange> convertValidityDate(
			ValidityDate selected,
			SqlFunctionProvider functionProvider, CDateRange dateRestriction) {

		if (selected == null) {
			return Optional.empty();
		}

		if (dateRestriction != null) {
			return Optional.of(functionProvider.forValidityDate(selected, dateRestriction).asValidityDateRange());
		}

		return Optional.of(functionProvider.forValidityDate(selected).asValidityDateRange());
	}

	private static boolean dateRestrictionApplicable(boolean dateRestrictionRequired, Optional<ColumnDateRange> validityDateSelect) {
		return dateRestrictionRequired && validityDateSelect.isPresent();
	}

	private static Condition connectorPreprocess(SqlIdColumns ids, ValidityDate validityDate, SqlFunctionProvider functionProvider) {
		List<Condition> conditions = new ArrayList<>();
		if (validityDate != null) {
			conditions.add(functionProvider.validityDateFilter(validityDate));
		}

		ids.getSecondaryId().ifPresent(sidField -> conditions.add(sidField.isNotNull()));

		return DSL.and(conditions);
	}

	private static Condition connectorCondition(
			Connector connector, SqlIdColumns ids, ValidityDate validityDate, SqlFunctionProvider functionProvider,
			CTConditionContext ctx) {
		Condition condition = connector.getCondition() == null ? noCondition() : connector.getCondition().convertToSqlCondition(ctx);
		Condition prerequisites = connectorPreprocess(ids, validityDate, functionProvider);

		return condition.and(prerequisites);
	}


	@NotNull
	private static Condition conceptElementCondition(List<ConceptElement<?>> conceptElements, CQTable cqTable, SqlFunctionProvider functionProvider) {
		List<Condition> conditions = new ArrayList<>();

		for (ConceptElement<?> conceptElement : conceptElements) {
			conditions.add(convertConceptElementCondition(conceptElement, cqTable, functionProvider));
		}

		return DSL.or(conditions);
	}

	private static Condition connectorPrerequisites(SqlIdColumns ids, ValidityDate validityDate, SqlFunctionProvider functionProvider) {
		List<Condition> conditions = new ArrayList<>();
		if (validityDate != null) {
			conditions.add(functionProvider.validityDateFilter(validityDate));
		}

		ids.getSecondaryId().ifPresent(sidField -> conditions.add(sidField.isNotNull()));

		return DSL.and(conditions);
	}

	/**
	 * Collects all conditions of a given {@link ConceptTreeChild} by resolving the condition of the given node and all of its parent nodes.
	 */
	private static Condition convertConceptElementCondition(ConceptElement<?> conceptElement, CQTable cqTable, SqlFunctionProvider functionProvider) {
		if (conceptElement instanceof ConceptTreeChild child) {
			Condition childCondition = child.getCondition().convertToSqlCondition(CTConditionContext.create(cqTable.getConnector().resolve(), functionProvider));
			Condition parentCondition = convertConceptElementCondition(child.getParent(), cqTable, functionProvider);

			return parentCondition.and(childCondition);
		}

		return DSL.noCondition();
	}

	private static Condition convertConnectorCondition(CQTable cqTable, SqlFunctionProvider functionProvider, SqlIdColumns ids) {

		final Connector connector = cqTable.getConnector().resolve();

		Condition prerequisites = connectorPrerequisites(ids, cqTable.findValidityDate(), functionProvider);

		if (connector.getCondition() == null) {
			return prerequisites;
		}
		Condition converted = connector.getCondition().convertToSqlCondition(CTConditionContext.create(connector, functionProvider));

		return converted.and(prerequisites);
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

		Condition dateRestrictionCondition = functionProvider.dateRestriction(validityDate.get(), dateRestriction);

		return Optional.of(new SqlFilters(
				ConnectorSqlSelects.builder().preprocessingSelects(dateRestrictionSelects).build(),
				WhereClauses.builder().eventFilter(ConditionUtil.wrap(dateRestrictionCondition)).build()
		));
	}

	private static ConnectorSqlSelects createConceptColumnConnectorSqlSelects(CQConcept cqConcept, SelectContext<ConnectorSqlTables> selectContext) {
		for (SelectId selectId : cqConcept.getSelects()) {
			Select resolve = selectId.resolve();
			if (resolve instanceof ConceptColumnSelect select) {
				return select.createConverter().connectorSelect(select, selectContext);
			}
		}

		return ConnectorSqlSelects.none();
	}

	@Override
	public Class<CQConcept> getConversionClass() {
		return CQConcept.class;
	}

	@Override
	public ConversionContext convert(CQConcept cqConcept, ConversionContext context) {

		TablePath tablePath = new TablePath(cqConcept, context);
		List<ConceptElement<?>> conceptElements = cqConcept.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList();

		List<SimplifiedQueryStep> convertedCQTables = new ArrayList<>();
		for (CQTable cqTable : cqConcept.getTables()) {
			SimplifiedQueryStep simplifiedQueryStep = convertCqTable(tablePath, cqConcept, cqTable, context,
																	 context.getSqlDialect().getFunctionProvider(),
																	 conceptElements
			);
			convertedCQTables.add(simplifiedQueryStep);
		}

		SimplifiedQueryStep joinedStep = QueryStepJoiner.joinSteps2(convertedCQTables, ConqueryJoinType.OUTER_JOIN, DateAggregationAction.MERGE, context);
		//QueryStep lastConceptStep = finishConceptConversion(joinedStep, cqConcept, tablePath, context);
		return context;
	}

	private SimplifiedQueryStep convertCqTable(
			TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext context, SqlFunctionProvider functionProvider,
			List<ConceptElement<?>> conceptElements) {

		//TODO rework QueryStep to hold actual DSL.select().from()... and the necessary context objects (e.g. ids, and selects)

		ConnectorSqlTables connectorTables = tablePath.getConnectorTables(cqTable);

		// Convert Ids
		SqlIdColumns ids = convertIds(cqConcept, cqTable, context);

		// Convert ValidityDate
		ValidityDate validityDate = cqTable.findValidityDate();
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(validityDate,
																		   functionProvider, context.getDateRestrictionRange()
		);

		List<Condition> eventFilterConditions = new ArrayList<>();
		List<Condition> havingConditions = new ArrayList<>();


		for (FilterValue<?> filterValue : cqTable.getFilters()) {
			eventFilterConditions.add(filterValue.convertEventFilter(ids, context, connectorTables));
			havingConditions.add(filterValue.convertHavingFilter(ids, context, connectorTables));
		}

		CTConditionContext conditionContext = CTConditionContext.create(cqTable.getConnector().resolve(), functionProvider);

		eventFilterConditions.add(conceptElementCondition(conceptElements, cqTable, functionProvider));
		eventFilterConditions.add(connectorCondition(connectorTables.getConnector(), ids, validityDate, functionProvider, conditionContext));

		if (context.dateRestrictionActive() && tablesValidityDate.isPresent()) {
			eventFilterConditions.add(functionProvider.dateRestriction(tablesValidityDate.get(), functionProvider.forCDateRange(context.getDateRestrictionRange())));
		}

		List<Field<?>> aggregationSelects = new ArrayList<>();

		for (ConnectorSelectId selectId : cqTable.getSelects()) {
			Select resolved = selectId.resolve();
			String fieldName = context.getNameGenerator().selectName(resolved);

			Field<?> converted = resolved.convert(connectorTables.getRootTable(), functionProvider, context)
										 .as(name(fieldName));

			aggregationSelects.add(converted);
		}

		Field<Object> validityDateField;

		if (tablesValidityDate.isPresent()) {
			//TODO properly implement access to validity date
			validityDateField = DSL.aggregate("range_agg", Object.class, tablesValidityDate.get().toFields().getFirst());
		}
		else {
			validityDateField = noField(Object.class);
		}

		SelectHavingConditionStep<Record> connectorQuery =
				select(ids.toFields())
						.select(validityDateField.as(VALIDITY_DATE))
						.select(aggregationSelects)
						.from(table(name(connectorTables.getRootTable())))
						.where(eventFilterConditions)
						.groupBy(ids.toFields())
						.having(havingConditions)
						;

		log.info("{}", connectorQuery);

		return new SimplifiedQueryStep(
				context.getNameGenerator().conceptConnectorName(cqConcept, cqTable.getConnector().get(), Locale.ENGLISH),
				connectorQuery.asTable(),
				ids,
				aggregationSelects
		);
	}

	private Optional<QueryStep> __convertCqTable(TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext context) {

		ConnectorSqlTables connectorTables = tablePath.getConnectorTables(cqTable);
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();

		// Convert Ids
		SqlIdColumns ids = convertIds(cqConcept, cqTable, context);

		// Convert ValidityDate
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(cqTable.findValidityDate(),
																		   context.getSqlDialect().getFunctionProvider(), context.getDateRestrictionRange()
		);

		// convert filters
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();

		for (FilterValue<?> filterValue : cqTable.getFilters()) {
			allSqlFiltersForTable.add(filterValue.convertToSqlFilter(ids, context, connectorTables));
		}

		List<ConceptElement<?>> resolvedConceptElements = cqConcept.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList();
		//		allSqlFiltersForTable.add(collectConceptConditions(resolvedConceptElements, cqTable, functionProvider, ids));

		getDateRestriction(context, tablesValidityDate).ifPresent(allSqlFiltersForTable::add);

		// convert selects
		SelectContext<ConnectorSqlTables> selectContext = SelectContext.create(ids, tablesValidityDate, connectorTables, context);
		List<ConnectorSqlSelects> allSelectsForTable = new ArrayList<>();
		ConnectorSqlSelects conceptColumnSelect = createConceptColumnConnectorSqlSelects(cqConcept, selectContext);
		allSelectsForTable.add(conceptColumnSelect);

		for (ConnectorSelectId connectorSelectId : cqTable.getSelects()) {
			Select select = connectorSelectId.resolve();
			ConnectorSqlSelects connectorSqlSelects = select.createConverter().connectorSelect(select, selectContext);
			allSelectsForTable.add(connectorSqlSelects);
		}

		CQTableContext tableContext = CQTableContext.builder()
													.ids(ids)
													.validityDate(tablesValidityDate)
													.sqlSelects(allSelectsForTable)
													.sqlFilters(allSqlFiltersForTable)
													.connectorTables(connectorTables)
													.conversionContext(context)
													.build();

		List<SqlSelect> forPreprocessing = tableContext.allSqlSelects().stream()
													   .flatMap(sqlSelects -> sqlSelects.getPreprocessingSelects().stream())
													   .toList();

		//TODO move aggregation and filtering into preprocessing.
		//TODO move date aggregation into preprocessing, probably needs to ignore hana for now
		//TODO move aggregationFilters into preprocessing using HAVING

		// all where clauses that don't require any preprocessing (connector/child conditions)
		List<Condition> conditions = new ArrayList<>();

		for (SqlFilters sqlFilter : tableContext.getSqlFilters()) {
			for (WhereCondition whereCondition : sqlFilter.getWhereClauses().getPreprocessingConditions()) {
				conditions.add(whereCondition.condition());
			}
		}

		// event Filter step
		conditions.addAll(EventFilterCte.collectEventFilterConditions(tableContext));


		Selects selects = Selects.builder()
								 .ids(tableContext.getIds())
								 .validityDate(tableContext.getValidityDate())
								 .sqlSelects(forPreprocessing)
								 .build();

		QueryStep eventFilterStep = QueryStep.builder()
											 .selects(selects)
											 .conditions(conditions)
											 .cteName(tableContext.getConnectorTables().cteName(ConceptCteStep.EVENT_FILTER))
											 .predecessors(Collections.emptyList())
											 .fromTable(QueryStep.toTable(tableContext.getConnectorTables().getRootTable()))
											 .build();


		//TODO
		//		if (tableContext.getConversionContext().isWithStratification()) {
		//			builder = PreprocessingCte.joinWithStratificationTable(forPreprocessing, conditions, tableContext);
		//		}
		//		else {
		//			builder = builder.fromTable(QueryStep.toTable(tableContext.getConnectorTables().getRootTable()));
		//		}


		tableContext = tableContext.withPrevious(eventFilterStep);

		// Aggregation Step
		List<SqlSelect> requiredInAggregationFilterStep = tableContext.allSqlSelects().stream()
																	  .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																	  .toList();

		Selects predecessorSelects = eventFilterStep.getQualifiedSelects();
		Selects aggregationSelectSelects = Selects.builder()
												  .ids(predecessorSelects.getIds())
												  .stratificationDate(predecessorSelects.getStratificationDate())
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();


		List<Field<?>> groupByFields = new ArrayList<>(predecessorSelects.getIds().toFields());

		if (predecessorSelects.getStratificationDate().isPresent()) {
			groupByFields.addAll(predecessorSelects.getStratificationDate().get().toFields());
		}


		QueryStep aggregationStep = QueryStep.builder()
											 .selects(aggregationSelectSelects)
											 .groupBy(groupByFields)
											 .cteName(tableContext.getConnectorTables().cteName(ConceptCteStep.AGGREGATION_SELECT))
											 .fromTable(QueryStep.toTable(eventFilterStep.getCteName()))
											 .predecessor(eventFilterStep)
											 .build();
		tableContext = tableContext.withPrevious(aggregationStep);


		QueryStep joinBranchesStep = new JoinBranchesCte().convertStep(tableContext)
														  .cteName(tableContext.getConnectorTables()
																			   .cteName(ConceptCteStep.JOIN_BRANCHES)).build();
		tableContext = tableContext.withPrevious(joinBranchesStep);

		// AggregationFilter Step

		List<Condition> aggregationFilterConditions = tableContext.getSqlFilters().stream()
																  .flatMap(conceptFilter -> conceptFilter.getWhereClauses().getGroupFilters().stream())
																  .map(WhereCondition::condition)
																  .toList();


		QueryStep aggregationFilterStep = QueryStep.builder()
												   .selects(AggregationFilterCte.collectSelects(tableContext))
												   .fromTable(QueryStep.toTable(joinBranchesStep.getCteName()))
												   .conditions(aggregationFilterConditions)
												   .predecessors(List.of(aggregationStep, joinBranchesStep))
												   .cteName(tableContext.getConnectorTables().cteName(ConceptCteStep.AGGREGATION_FILTER)).build();

		return Optional.of(aggregationFilterStep);
	}

	private CQTableContext createTableContext(TablePath tablePath, CQConcept cqConcept, CQTable cqTable, ConversionContext conversionContext) {

		ConnectorSqlTables connectorTables = tablePath.getConnectorTables(cqTable);

		// Convert Ids
		SqlIdColumns ids = convertIds(cqConcept, cqTable, conversionContext);

		// Convert ValidityDate
		Optional<ColumnDateRange> tablesValidityDate = convertValidityDate(cqTable.findValidityDate(),
																		   conversionContext.getSqlDialect().getFunctionProvider(),
																		   conversionContext.getDateRestrictionRange()
		);

		// convert filters
		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();
		List<SqlFilters> allSqlFiltersForTable = new ArrayList<>();

		for (FilterValue<?> filterValue : cqTable.getFilters()) {
			allSqlFiltersForTable.add(filterValue.convertToSqlFilter(ids, conversionContext, connectorTables));
		}

		List<ConceptElement<?>> resolvedConceptElements = cqConcept.getElements().stream().<ConceptElement<?>>map(ConceptElementId::resolve).toList();
		//		allSqlFiltersForTable.add(collectConceptConditions(resolvedConceptElements, cqTable, functionProvider, ids));

		getDateRestriction(conversionContext, tablesValidityDate).ifPresent(allSqlFiltersForTable::add);

		// convert selects
		SelectContext<ConnectorSqlTables> selectContext = SelectContext.create(ids, tablesValidityDate, connectorTables, conversionContext);
		List<ConnectorSqlSelects> allSelectsForTable = new ArrayList<>();
		ConnectorSqlSelects conceptColumnSelect = createConceptColumnConnectorSqlSelects(cqConcept, selectContext);
		allSelectsForTable.add(conceptColumnSelect);

		for (ConnectorSelectId connectorSelectId : cqTable.getSelects()) {
			Select select = connectorSelectId.resolve();
			ConnectorSqlSelects connectorSqlSelects = select.createConverter().connectorSelect(select, selectContext);
			allSelectsForTable.add(connectorSqlSelects);
		}

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
