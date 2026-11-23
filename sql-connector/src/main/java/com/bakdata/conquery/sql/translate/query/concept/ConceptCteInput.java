package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import lombok.NonNull;

/** Fully SQL-resolved input needed to compile the final CTE of a concept node. */
public record ConceptCteInput(
		@NonNull QueryStep predecessor,
		List<ConceptSqlSelects> sqlSelects,
		@NonNull SqlTables tables,
		boolean negate,
		boolean blockValidityDate
) {

	public ConceptCteInput {
		sqlSelects = List.copyOf(sqlSelects);
	}
}
