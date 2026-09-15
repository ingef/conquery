package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;

/** Fully SQL-resolved input needed to join the branches of a connector concept. */
public record JoinBranchesCteInput(
		QueryStep predecessor,
		ColumnDateRange validityDate,
		SqlTables tables,
		boolean withIntervalPacking,
		boolean excludedFromTimeAggregation,
		List<SqlSelect> eventDateSelects,
		List<QueryStep> additionalPredecessors
) {

	public JoinBranchesCteInput {
		Objects.requireNonNull(predecessor, "predecessor");
		Objects.requireNonNull(validityDate, "validityDate");
		Objects.requireNonNull(tables, "tables");
		eventDateSelects = List.copyOf(eventDateSelects);
		additionalPredecessors = List.copyOf(additionalPredecessors);
	}
}
