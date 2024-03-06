package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum DateAggregationCteStep implements CteStep {

	// merge or intersect
	OVERLAP("overlap", OverlapCte::new, null),
	INTERMEDIATE_TABLE("no_overlap", IntermediateTableCte::new, null),
	NODE_NO_OVERLAP("node_no_overlap", NodeNoOverlapCte::new, INTERMEDIATE_TABLE),
	MERGE("merge", MergeCte::new, OVERLAP),

	// invert
	ROW_NUMBER("row_numbers", RowNumberCte::new, null),
	INVERT("inverted_dates", InvertCte::new, ROW_NUMBER);

	private static final List<DateAggregationCteStep> MERGE_STEPS = List.of(
			OVERLAP,
			INTERMEDIATE_TABLE,
			NODE_NO_OVERLAP,
			MERGE
	);

	private static final List<DateAggregationCteStep> INTERSECT_STEPS = List.of(
			OVERLAP,
			INTERMEDIATE_TABLE,
			MERGE
	);

	private static final List<DateAggregationCteStep> INVERT_STEPS = List.of(
			ROW_NUMBER,
			INVERT
	);

	private final String suffix;
	private final DateAggregationCteConstructor stepConstructor;
	private final CteStep predecessor;

	public static List<DateAggregationCte> createMergeCtes() {
		return createCtes(MERGE_STEPS);
	}

	public static List<DateAggregationCte> createIntersectCtes() {
		return createCtes(INTERSECT_STEPS);
	}

	public static List<DateAggregationCte> createInvertCtes() {
		return createCtes(INVERT_STEPS);
	}

	public static SqlTables createMergeTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return createTables(MERGE_STEPS, joinedTable, nameGenerator);
	}

	public static SqlTables createIntersectTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return createTables(INTERSECT_STEPS, joinedTable, nameGenerator);
	}

	public static SqlTables createInvertTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return createTables(INVERT_STEPS, joinedTable, nameGenerator);
	}

	private static List<DateAggregationCte> createCtes(List<DateAggregationCteStep> requiredSteps) {
		return requiredSteps.stream()
							.map(cteStep -> cteStep.getStepConstructor().create(cteStep))
							.toList();
	}

	private static SqlTables createTables(List<? extends CteStep> requiredSteps, QueryStep joinedTable, NameGenerator nameGenerator) {
		Set<? extends CteStep> asSet = new HashSet<>(requiredSteps);
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(asSet, joinedTable.getCteName(), nameGenerator);
		Map<CteStep, CteStep> predecessorMap = CteStep.getDefaultPredecessorMap(asSet);
		return new SqlTables(joinedTable.getCteName(), cteNameMap, predecessorMap);
	}


}
