package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum InvertCteStep implements DateAggregationCteStep {

	ROW_NUMBER("_row_numbers", RowNumberCte::new, null),
	INVERT("_inverted_dates", InvertCte::new, InvertCteStep.ROW_NUMBER);

	private static final Set<InvertCteStep> REQUIRED_STEPS = Set.of(values());
	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;
	private final InvertCteStep predecessor;

	@Override
	public String suffix() {
		return this.suffix;
	}

	@Override
	public DateAggregationCteStep predecessor() {
		return this.predecessor;
	}

	static List<DateAggregationCte> requiredSteps() {
		return Arrays.stream(values())
					 .map(cteStep -> cteStep.getStepConstructor().create(cteStep))
					 .toList();
	}

	static DateAggregationTables<InvertCteStep> createTableNames(QueryStep joinedTable) {
		return new DateAggregationTables<>(joinedTable.getCteName(), REQUIRED_STEPS, joinedTable.getCteName());
	}

}
