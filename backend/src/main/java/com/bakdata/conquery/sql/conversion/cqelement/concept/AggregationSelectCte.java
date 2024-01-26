package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> requiredInAggregationFilterStep = tableContext.getSqlSelects().stream()
																	  .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																	  .distinct()
																	  .toList();

		Selects aggregationSelectSelects = Selects.builder()
												  .primaryColumn(tableContext.getPrimaryColumn())
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(List.of(tableContext.getPrimaryColumn()));
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.AGGREGATION_SELECT;
	}

}
