package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum MergeStep implements DateAggregationStep {

	OVERLAP("_overlap", OverlapCte::new),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new),
	NODE_NO_OVERLAP("_node_no_overlap", NodeNoOverlapCte::new),
	MERGE("_merge", MergeCte::new);

	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;

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
		predecessorMap.put(NODE_NO_OVERLAP, cteNameMap.get(INTERMEDIATE_TABLE));
		predecessorMap.put(MERGE, cteNameMap.get(OVERLAP));
		return new DateAggregationTables(cteNameMap, predecessorMap);
	}

}
