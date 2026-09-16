package com.bakdata.conquery.sql.compiler.ir.interval;

import java.util.Objects;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;

/** SQL-ready interval-packing state used to construct one derived select branch. */
public record IntervalPackingSelectPreparation(
		QueryStep predecessor,
		ColumnDateRange dateRange,
		SqlTables tables
) {

	public IntervalPackingSelectPreparation {
		Objects.requireNonNull(predecessor, "predecessor");
		Objects.requireNonNull(dateRange, "dateRange");
		Objects.requireNonNull(tables, "tables");
	}
}
