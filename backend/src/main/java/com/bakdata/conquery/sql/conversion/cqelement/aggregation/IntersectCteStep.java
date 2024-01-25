package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum IntersectCteStep implements DateAggregationCteStep {

	OVERLAP("_overlap", OverlapCte::new, null),
	INTERMEDIATE_TABLE("_no_overlap", IntermediateTableCte::new, null),
	MERGE("_merge", MergeCte::new, OVERLAP);

	private static final Set<IntersectCteStep> REQUIRED_STEPS = Set.of(values());
	private final String suffix;
	private final DateAggregationCteConstructor stepConstructor;
	private final IntersectCteStep predecessor;

	static List<DateAggregationCte> requiredSteps() {
		return Arrays.stream(values())
					 .map(cteStep -> cteStep.getStepConstructor().create(cteStep))
					 .toList();
	}

	static DateAggregationTables<IntersectCteStep> createTableNames(QueryStep joinedTable, NameGenerator nameGenerator) {
		return new DateAggregationTables<>(joinedTable.getCteName(), REQUIRED_STEPS, joinedTable.getCteName(), nameGenerator);
	}

}
