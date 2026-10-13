package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;

/** Fully SQL-resolved input needed to compile the final CTE of a concept node. */
public record ConceptCteInput(
		QueryStep predecessor,
		List<ConceptSqlSelects> sqlSelects,
		SqlTables tables,
		boolean negate,
		boolean blockValidityDate
) {

	public ConceptCteInput {
		Objects.requireNonNull(predecessor, "predecessor");
		sqlSelects = List.copyOf(sqlSelects);
		Objects.requireNonNull(tables, "tables");
	}
}
