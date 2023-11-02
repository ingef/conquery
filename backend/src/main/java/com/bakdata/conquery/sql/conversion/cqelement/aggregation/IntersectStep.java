package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum IntersectStep implements DateAggregationStep {

	OVERLAP("_overlap", OverlapCte::new, null),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new, null),
	MERGE("_merge", MergeCte::new, OVERLAP);

	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;
	private final IntersectStep predecessor;

	@Override
	public String suffix() {
		return this.suffix;
	}

	@Override
	public DateAggregationStep predecessor() {
		return this.predecessor;
	}

	static List<DateAggregationCte> requiredSteps() {
		return Arrays.stream(values())
					 .map(cteStep -> cteStep.getStepConstructor().create(cteStep))
					 .toList();
	}

	static DateAggregationTables tableNames(QueryStep joinedTable) {
		Map<DateAggregationStep, String> cteNameMap = DateAggregationStep.createCteNameMap(joinedTable, values());
		return new DateAggregationTables(joinedTable.getCteName(), cteNameMap);
	}

}
