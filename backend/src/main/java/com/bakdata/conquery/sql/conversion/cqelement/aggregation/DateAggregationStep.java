package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import com.bakdata.conquery.sql.conversion.model.CteStep;

interface DateAggregationStep extends CteStep {

	String suffix();

	DateAggregationStep predecessor();

	@Override
    default String cteName(String nodeLabel) {
		return "%s%s".formatted(nodeLabel, suffix());
	}

}
