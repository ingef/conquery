package com.bakdata.conquery.sql.translate.query.interval;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import lombok.NonNull;

/** SQL-ready interval-packing state used to construct one derived select branch. */
public record IntervalPackingSelectPreparation(
		@NonNull QueryStep predecessor,
		@NonNull ColumnDateRange dateRange,
		@NonNull SqlTables tables
) {
}
