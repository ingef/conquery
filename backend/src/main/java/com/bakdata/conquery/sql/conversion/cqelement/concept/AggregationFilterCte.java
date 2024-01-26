package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class AggregationFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		String predecessorTableName = tableContext.getConceptTables().getPredecessor(cteStep());
		Selects aggregationFilterSelects = Selects.builder()
												  .primaryColumn(tableContext.getPrimaryColumn())
												  .sqlSelects(getForAggregationFilterSelects(tableContext))
												  .build()
												  .qualify(predecessorTableName);

		List<Condition> aggregationFilterConditions = tableContext.getSqlFilters().stream()
																  .flatMap(conceptFilter -> conceptFilter.getWhereClauses().getGroupFilters().stream())
																  .map(WhereCondition::condition)
																  .toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private List<SqlSelect> getForAggregationFilterSelects(CQTableContext tableContext) {
		return tableContext.getSqlSelects().stream()
						   .flatMap(sqlSelects -> sqlSelects.getFinalSelects().stream())
						   .filter(sqlSelect -> !sqlSelect.isUniversal())
						   .distinct()
						   .toList();
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.AGGREGATION_FILTER;
	}

}
