package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import com.bakdata.conquery.sql.conversion.model.CteStep;

interface DateAggregationCteStep extends CteStep {

	@Override
	default String cteName(String nodeLabel) {
		return "%s%s".formatted(nodeLabel, getSuffix());
	}

}
