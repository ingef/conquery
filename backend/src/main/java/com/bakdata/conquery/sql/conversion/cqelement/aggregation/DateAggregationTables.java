package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Map;

import lombok.Value;

@Value
class DateAggregationTables {

	Map<DateAggregationStep, String> cteNameMap;
	Map<DateAggregationStep, String> predecessorMap;

	public DateAggregationTables(
			Map<DateAggregationStep, String> cteNameMap,
			Map<DateAggregationStep, String> predecessorMap
	) {
		this.cteNameMap = cteNameMap;
		this.predecessorMap = predecessorMap;
	}

	public String cteName(DateAggregationStep dateAggregationStep) {
		return this.cteNameMap.get(dateAggregationStep);
	}

	public String getFromTableOf(DateAggregationStep dateAggregationStep) {
		return this.predecessorMap.get(dateAggregationStep);
	}

}
