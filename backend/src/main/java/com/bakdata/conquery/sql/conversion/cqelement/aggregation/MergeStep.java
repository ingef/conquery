package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum MergeStep implements DateAggregationStep {

	OVERLAP("_overlap", OverlapCte::new, null),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new, null),
	NODE_NO_OVERLAP("_node_no_overlap", NodeNoOverlapCte::new, INTERMEDIATE_TABLE),
	MERGE("_merge", MergeCte::new, OVERLAP);

	private static final Set<MergeStep> REQUIRED_STEPS = Set.of(values());
	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;
	private final MergeStep predecessor;

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

	static DateAggregationTables<MergeStep> tableNames(QueryStep joinedTable) {
		return new DateAggregationTables<>(joinedTable.getCteName(), REQUIRED_STEPS, joinedTable.getCteName());
	}

}
