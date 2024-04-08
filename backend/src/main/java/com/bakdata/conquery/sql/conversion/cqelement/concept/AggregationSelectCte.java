package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		String predecessor = tableContext.getConnectorTables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		SqlIdColumns ids = tableContext.getIds().qualify(predecessor);

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
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_SELECT;
	}

}
