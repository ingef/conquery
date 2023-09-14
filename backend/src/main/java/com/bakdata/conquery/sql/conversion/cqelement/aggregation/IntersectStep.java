package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum IntersectStep implements DateAggregationStep {

	OVERLAP("_overlap", OverlapCte::new),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new),
	MERGE("_merge", MergeCte::new);

	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;

	@Override
	public String suffix() {
		return this.suffix;
	}

	static List<DateAggregationCte> requiredSteps() {
		return Arrays.stream(values())
					 .map(cteStep -> cteStep.getStepConstructor().create(cteStep))
					 .toList();
	}

	static DateAggregationTables tableNames(QueryStep joinedTable) {
		Map<DateAggregationStep, String> cteNameMap = DateAggregationStep.createCteNameMap(joinedTable, values());
		Map<DateAggregationStep, String> predecessorMap = new HashMap<>();
		predecessorMap.put(OVERLAP, joinedTable.getCteName());
		predecessorMap.put(INTERMEDIATE_TABLE, joinedTable.getCteName());
		predecessorMap.put(MERGE, cteNameMap.get(OVERLAP));
		return new DateAggregationTables(cteNameMap, predecessorMap);
	}

}
