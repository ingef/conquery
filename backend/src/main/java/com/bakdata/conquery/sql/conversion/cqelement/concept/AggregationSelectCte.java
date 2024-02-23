package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SelectsIds;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		String predecessor = tableContext.getConnectorTables().getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
		SelectsIds ids = tableContext.getIds().qualify(predecessor);

		List<SqlSelect> requiredInAggregationFilterStep = tableContext.allSqlSelects().stream()
																	  .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																	  .toList();

		Selects aggregationSelectSelects = Selects.builder()
												  .ids(ids)
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(ids.toFields());
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.AGGREGATION_SELECT;
	}

}
