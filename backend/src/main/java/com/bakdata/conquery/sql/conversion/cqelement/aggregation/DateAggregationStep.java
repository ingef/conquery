package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;

interface DateAggregationStep extends CteStep {

	static Map<DateAggregationStep, String> createCteNameMap(
			QueryStep joinedTable,
			DateAggregationStep[] dateAggregationSteps
	) {
		return Arrays.stream(dateAggregationSteps)
					 .collect(Collectors.toMap(
							 Function.identity(),
							 dateAggregationStep -> "%s%s".formatted(joinedTable.getCteName(), dateAggregationStep.suffix())
					 ));
	}

}
