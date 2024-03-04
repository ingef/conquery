package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
enum InvertCteStep implements DateAggregationCteStep {

	ROW_NUMBER("row_numbers", RowNumberCte::new, null),
	INVERT("inverted_dates", InvertCte::new, InvertCteStep.ROW_NUMBER);

	private static final Set<InvertCteStep> REQUIRED_STEPS = Set.of(values());
	private final String suffix;
	private final DateAggregationCteConstructor stepConstructor;
	private final InvertCteStep predecessor;

	static List<DateAggregationCte> requiredSteps() {
		return Arrays.stream(values())
					 .map(cteStep -> cteStep.getStepConstructor().create(cteStep))
					 .toList();
	}

	static SqlTables getTables(QueryStep joinedTable, NameGenerator nameGenerator) {
		return new SqlTables(joinedTable.getCteName(), REQUIRED_STEPS, joinedTable.getCteName(), nameGenerator);
	}

}
