package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum IntersectStep implements DateAggregationStep {

	OVERLAP("_overlap", OverlapCte::new, null),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new, null),
	MERGE("_merge", MergeCte::new, OVERLAP);

	private static final Set<IntersectStep> REQUIRED_STEPS = Arrays.stream(values()).collect(Collectors.toSet());
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

	static DateAggregationTables<IntersectStep> createTableNames(QueryStep joinedTable) {
		return new DateAggregationTables<>(joinedTable.getCteName(), REQUIRED_STEPS, joinedTable.getCteName());
	}

}
