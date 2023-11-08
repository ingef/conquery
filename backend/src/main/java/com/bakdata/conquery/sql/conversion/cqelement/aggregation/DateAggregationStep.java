package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;

interface DateAggregationStep extends CteStep {

	String suffix();

	DateAggregationStep predecessor();

	static Map<DateAggregationStep, String> createCteNameMap(QueryStep joinedTable, DateAggregationStep[] dateAggregationSteps) {
		return Arrays.stream(dateAggregationSteps)
					 .collect(Collectors.toMap(
							 Function.identity(),
							 dateAggregationStep -> dateAggregationStep.cteName(joinedTable.getCteName())
					 ));
	}

	@Override
    default String cteName(String nodeLabel) {
		return "%s%s".formatted(nodeLabel, suffix());
	};

}
