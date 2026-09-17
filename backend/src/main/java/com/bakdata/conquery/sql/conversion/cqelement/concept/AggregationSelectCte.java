package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;

class AggregationSelectCte extends ConnectorCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<SqlSelect> requiredInAggregationFilterStep = tableContext.allSqlSelects().stream()
																	  .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																	  .toList();

		Selects predecessorSelects = tableContext.getPrevious().getQualifiedSelects();
		SqlIdColumns ids = predecessorSelects.getIds();
		Optional<ColumnDateRange> stratificationDate = predecessorSelects.getStratificationDate();
		Selects aggregationSelectSelects = Selects.builder()
												  .ids(ids)
												  .stratificationDate(stratificationDate)
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();

		List<Field<?>> groupByFields = Stream.concat(
													 ids.toFields().stream(),
													 stratificationDate.stream().flatMap(range -> range.toFields().stream())
											 )
											 .toList();

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(groupByFields);
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_SELECT;
	}

}
