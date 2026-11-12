package com.bakdata.conquery.sql.translate.query.interval;

import java.util.Objects;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;

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
