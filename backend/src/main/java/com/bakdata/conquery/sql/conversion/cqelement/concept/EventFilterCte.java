package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.Field;

class EventFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<Condition> conditions = new ArrayList<>();

		conditions.addAll(tableContext.getIds().toFields().stream().map(Field::isNotNull).toList());

		Selects eventFilterSelects = collectSelects(tableContext);
		List<Condition> eventFilterConditions = collectEventFilterConditions(tableContext);

		conditions.addAll(eventFilterConditions);

		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(conditions);
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.EVENT_FILTER;
	}

	private Selects collectSelects(CQTableContext tableContext) {

		String predecessorTableName = tableContext.getPrevious().getCteName();
		Selects predecessorSelects = tableContext.getPrevious().getQualifiedSelects();

		SqlIdColumns ids = predecessorSelects.getIds();
		Optional<ColumnDateRange> validityDate = predecessorSelects.getValidityDate();
		Optional<ColumnDateRange> stratificationDate = predecessorSelects.getStratificationDate();

		List<? extends SqlSelect> eventFilterSelects =
				tableContext.allSqlSelects().stream()
							.flatMap(sqlSelects -> collectSelects(sqlSelects).stream())
							.flatMap(sqlSelect -> referenceRequiredColumns(sqlSelect, predecessorTableName))
							.toList();

		return Selects.builder()
					  .ids(ids)
					  .validityDate(validityDate)
					  .stratificationDate(stratificationDate)
					  .sqlSelects(eventFilterSelects)
					  .build();
	}

	/**
	 * Collects the columns required in {@link ConceptCteStep#AGGREGATION_SELECT}, the optional connector column, but also columns additional tables require
	 * (like the ones created by the {@link SumSqlAggregator}) when distinct-by columns are present. An additional predecessor can contain an N-ary tree of
	 * predecessors itself (like all {@link QueryStep}s), so we want to look for the deepest preceding QueryStep leafs and collect their
	 * {@link ConnectorSqlSelects}, because they expect this CTE to contain all their {@link SqlSelect#requiredColumns()}.
	 */
	private static List<SqlSelect> collectSelects(ConnectorSqlSelects sqlSelects) {
		return Stream.concat(
							 sqlSelects.getConnectorColumn().stream(),
							 Stream.concat(
									 sqlSelects.getAggregationSelects().stream(),
									 sqlSelects.getAdditionalPredecessor().map(EventFilterCte::collectDeepestPredecessorsColumns).orElse(Stream.empty())
							 )
					 )
					 .toList();
	}

	/**
	 * Recursively looks for the deepest predecessors of the given additional predecessor and collects their {@link SqlSelect}.
	 */
	private static Stream<SqlSelect> collectDeepestPredecessorsColumns(QueryStep additionalPredecessor) {
		if (additionalPredecessor.getPredecessors().isEmpty()) {
			return Stream.concat(
					additionalPredecessor.getSelects().getSqlSelects().stream(),
					additionalPredecessor.getGroupBy().stream().map(FieldWrapper::new) // some required columns may just be referenced in a GROUP BY statement
			);
		}
		return additionalPredecessor.getPredecessors().stream().flatMap(EventFilterCte::collectDeepestPredecessorsColumns);
	}

	private static Stream<ExtractingSqlSelect<?>> referenceRequiredColumns(SqlSelect sqlSelect, String predecessorTableName) {
		return sqlSelect.requiredColumns().stream().map(column -> new ExtractingSqlSelect<>(predecessorTableName, column, Object.class));
	}

	private static List<Condition> collectEventFilterConditions(CQTableContext tableContext) {

		List<Condition> eventFilterConditions = tableContext.getSqlFilters().stream()
															.flatMap(conceptFilter -> conceptFilter.getWhereClauses().getEventFilters().stream())
															.map(WhereCondition::condition)
															.toList();

		if (!tableContext.getConversionContext().isWithStratification()) {
			return eventFilterConditions;
		}
		return addStratificationCondition(eventFilterConditions, tableContext);
	}

	private static List<Condition> addStratificationCondition(List<Condition> eventFilterConditions, CQTableContext tableContext) {
		Selects previousSelects = tableContext.getPrevious().getQualifiedSelects();
		Preconditions.checkArgument(
				previousSelects.getStratificationDate().isPresent() && previousSelects.getValidityDate().isPresent(),
				"Can't apply stratification for table %s".formatted(tableContext.getConnectorTables().getRootTable())
		);

		// we filter every entry where stratification date range and validity date range do not overlap
		SqlFunctionProvider functionProvider = tableContext.getConversionContext().getSqlDialect().getFunctionProvider();
		ColumnDateRange stratificationDate = previousSelects.getStratificationDate().get();
		ColumnDateRange validityDate = previousSelects.getValidityDate().get();
		Condition stratificationCondition = functionProvider.dateRestriction(stratificationDate, validityDate);

		return Stream.concat(Stream.of(stratificationCondition), eventFilterConditions.stream()).toList();
	}

}
