package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Map;

import lombok.Value;

@Value
class DateAggregationTables {

	String joinedTable;
	Map<DateAggregationStep, String> cteNameMap;

	public DateAggregationTables(String joinedTable, Map<DateAggregationStep, String> cteNameMap) {
		this.joinedTable = joinedTable;
		this.cteNameMap = cteNameMap;
	}

	public String cteName(DateAggregationStep dateAggregationStep) {
		return this.cteNameMap.get(dateAggregationStep);
	}

	public String getFromTableOf(DateAggregationStep dateAggregationStep) {
		DateAggregationStep predecessor = dateAggregationStep.predecessor();
		if (predecessor == null) {
			return joinedTable;
		}
		return this.cteNameMap.getOrDefault(predecessor, this.cteNameMap.get(predecessor.predecessor()));
	}

}
