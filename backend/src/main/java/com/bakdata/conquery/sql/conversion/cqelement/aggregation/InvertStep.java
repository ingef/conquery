package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum InvertStep implements DateAggregationStep {

	ROW_NUMBER("_row_numbers", RowNumberCte::new, null),
	INVERT("_inverted_dates", InvertCte::new, InvertStep.ROW_NUMBER);

	private final String suffix;
	@Getter
	private final DateAggregationCteConstructor stepConstructor;
	private final InvertStep predecessor;

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

	static DateAggregationTables<InvertStep> createTableNames(QueryStep joinedTable) {
		Map<InvertStep, String> cteNameMap = DateAggregationStep.createCteNameMap(joinedTable, values());
		return new DateAggregationTables<>(cteNameMap, joinedTable.getCteName());
	}

}
