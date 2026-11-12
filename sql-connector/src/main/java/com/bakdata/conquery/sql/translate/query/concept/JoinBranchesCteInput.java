package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;

/** Fully SQL-resolved input needed to join the branches of a connector concept. */
public record JoinBranchesCteInput(
		QueryStep predecessor,
		ColumnDateRange validityDate,
		SqlTables tables,
		boolean withIntervalPacking,
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
