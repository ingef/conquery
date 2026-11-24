package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.NonNull;

/** Fully SQL-resolved input needed to join the branches of a connector concept. */
public record JoinBranchesCteInput(
		@NonNull QueryStep predecessor,
		@NonNull ColumnDateRange validityDate,
		@NonNull SqlTables tables,
		boolean withIntervalPacking,
		List<SqlSelect> eventDateSelects,
		List<QueryStep> additionalPredecessors
) {

	public JoinBranchesCteInput {
		eventDateSelects = List.copyOf(eventDateSelects);
		additionalPredecessors = List.copyOf(additionalPredecessors);
	}
}
