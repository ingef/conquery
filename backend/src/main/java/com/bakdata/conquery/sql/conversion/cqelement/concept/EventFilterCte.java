package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumDistinctSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import org.jooq.Condition;

class EventFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {
		Selects eventFilterSelects = getEventFilterSelects(tableContext);
		List<Condition> eventFilterConditions = tableContext.getSqlFilters().stream()
															.flatMap(conceptFilter -> conceptFilter.getWhereClauses().getEventFilters().stream())
															.map(WhereCondition::condition)
															.toList();
		return QueryStep.builder()
						.selects(eventFilterSelects)
						.conditions(eventFilterConditions);
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.EVENT_FILTER;
	}

	private Selects getEventFilterSelects(CQTableContext tableContext) {
		String predecessorTableName = tableContext.getConnectorTables().getPredecessor(cteStep());
		SqlIdColumns ids = tableContext.getIds().qualify(predecessorTableName);

		Optional<ColumnDateRange> validityDate = tableContext.getValidityDate();
		if (validityDate.isPresent()) {
			validityDate = Optional.of(validityDate.get().qualify(predecessorTableName));
		}

		List<? extends SqlSelect> eventFilterSelects =
				tableContext.allSqlSelects().stream()
							.flatMap(sqlSelects -> collectForEventFilterStep(sqlSelects).stream())
							.flatMap(sqlSelect -> referenceRequiredColumns(sqlSelect, predecessorTableName))
							.toList();

		return Selects.builder()
					  .ids(ids)
					  .validityDate(validityDate)
					  .sqlSelects(eventFilterSelects)
					  .build();
	}

	/**
	 * Collects the columns required in {@link ConnectorCteStep#AGGREGATION_SELECT}, but also columns additional tables require (like the ones created by the
	 * {@link SumDistinctSqlAggregator}). An additional predecessor can contain an N-ary tree of predecessors itself (like all {@link QueryStep}s), so we want to
	 * look for the deepest predeceasing QueryStep leafs and collect their {@link SqlSelects}, because they expect this CTE to contain all their
	 * {@link SqlSelect#requiredColumns()}.
	 */
	private static List<SqlSelect> collectForEventFilterStep(SqlSelects sqlSelects) {
		return Stream.concat(
							 sqlSelects.getAggregationSelects().stream(),
							 sqlSelects.getAdditionalPredecessor().map(EventFilterCte::collectDeepestPredecessorsColumns).orElse(Stream.empty())
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

}
