package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.NonNull;

/** Fully SQL-resolved input needed to compile one connector's concept CTE pipeline. */
public record ConnectorCtePipelineInput(
		@NonNull SqlTables tables,
		@NonNull PreprocessingCteInput preprocessing,
		List<SqlSelect> eventDateSelects,
		List<QueryStep> additionalPredecessors,
		boolean withIntervalPacking
) {

	public ConnectorCtePipelineInput {
		eventDateSelects = List.copyOf(eventDateSelects);
		additionalPredecessors = List.copyOf(additionalPredecessors);
	}
}
