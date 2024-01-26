package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;

class AggregationFilterCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext CQTableContext) {

		String predecessorTableName = CQTableContext.getConceptTables().getPredecessor(cteStep());
		Selects aggregationFilterSelects = Selects.builder()
												  .primaryColumn(CQTableContext.getPrimaryColumn())
												  .sqlSelects(getForAggregationFilterSelects(CQTableContext))
												  .build()
												  .qualify(predecessorTableName);

		List<Condition> aggregationFilterConditions = CQTableContext.getFilters().stream()
																	.flatMap(conceptFilter -> conceptFilter.getWhereClauses().getGroupFilters().stream())
																	.map(WhereCondition::condition)
																	.toList();

		return QueryStep.builder()
						.selects(aggregationFilterSelects)
						.conditions(aggregationFilterConditions);
	}

	private List<SqlSelect> getForAggregationFilterSelects(CQTableContext CQTableContext) {
		return CQTableContext.getSelects().stream()
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
