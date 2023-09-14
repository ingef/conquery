package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum InvertStep implements DateAggregationStep {

	ROW_NUMBER("_row_numbers", RowNumberCte::new),
	INVERT("_inverted_dates", InvertCte::new);

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

	static DateAggregationTables createTableNames(QueryStep joinedTable) {
		Map<DateAggregationStep, String> cteNameMap = DateAggregationStep.createCteNameMap(joinedTable, values());
		Map<DateAggregationStep, String> predecessorMap = new HashMap<>();
		predecessorMap.put(ROW_NUMBER, joinedTable.getCteName());
		predecessorMap.put(INVERT, cteNameMap.get(ROW_NUMBER));
		return new DateAggregationTables(cteNameMap, predecessorMap);
	}

}
