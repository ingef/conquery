package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;

/** Fully SQL-resolved input needed to compile the final CTE of a concept node. */
public record ConceptCteInput(
		QueryStep predecessor,
		List<ConceptSqlSelects> sqlSelects,
		SqlTables tables,
		boolean negate
) {

	public ConceptCteInput {
		Objects.requireNonNull(predecessor, "predecessor");
		sqlSelects = List.copyOf(sqlSelects);
		Objects.requireNonNull(tables, "tables");
	}
}
