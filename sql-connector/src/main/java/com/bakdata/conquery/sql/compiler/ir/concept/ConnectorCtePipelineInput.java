package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;

/** Fully SQL-resolved input needed to compile one connector's concept CTE pipeline. */
public record ConnectorCtePipelineInput(
		SqlTables tables,
		PreprocessingCteInput preprocessing,
		List<SqlSelect> eventDateSelects,
		List<QueryStep> additionalPredecessors,
		boolean withIntervalPacking,
		boolean excludedFromTimeAggregation
) {

	public ConnectorCtePipelineInput {
		Objects.requireNonNull(tables, "tables");
		Objects.requireNonNull(preprocessing, "preprocessing");
		eventDateSelects = List.copyOf(eventDateSelects);
		additionalPredecessors = List.copyOf(additionalPredecessors);
	}
}
