package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		String predecessor = tableContext.getConnectorTables().getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
		Field<Object> primaryColumn = QualifyingUtil.qualify(tableContext.getPrimaryColumn(), predecessor);

		List<SqlSelect> requiredInAggregationFilterStep = tableContext.allSqlSelects().stream()
																	  .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																	  .toList();

		Selects aggregationSelectSelects = Selects.builder()
												  .primaryColumn(primaryColumn)
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(List.of(primaryColumn));
	}

	@Override
	public ConnectorCteStep cteStep() {
		return ConnectorCteStep.AGGREGATION_SELECT;
	}

}
