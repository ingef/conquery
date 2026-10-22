package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

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

	// merge
	INTERMEDIATE_TABLE("no_overlap", null),
	MERGE("merge", INTERMEDIATE_TABLE),

	// invert
	ROW_NUMBER("row_numbers", null),
	INVERT("inverted_dates", ROW_NUMBER);

	private static final Set<DateAggregationCteStep> MERGE_STEPS = Set.of(INTERMEDIATE_TABLE, MERGE);
	private static final Set<DateAggregationCteStep> INVERT_STEPS = Set.of(ROW_NUMBER, INVERT);

	private final String suffix;
	private final CteStep predecessor;

	public static SqlTables createMergeTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return createTables(MERGE_STEPS, joinedTable, nameGenerator);
	}

	public static SqlTables createInvertTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return createTables(INVERT_STEPS, joinedTable, nameGenerator);
	}

	private static SqlTables createTables(Set<? extends CteStep> requiredSteps, QueryStep joinedTable, NameGenerator nameGenerator) {
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(requiredSteps, joinedTable.getCteName(), nameGenerator);
		Map<CteStep, CteStep> predecessorMap = CteStep.getDefaultPredecessorMap(requiredSteps);
		return new SqlTables(joinedTable.getCteName(), cteNameMap, predecessorMap);
	}


}
