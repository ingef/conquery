package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import org.jooq.Record;
import org.jooq.Table;

/** Fully SQL-resolved input needed to build a connector preprocessing CTE. */
public record PreprocessingCteInput(
		Table<Record> sourceTable,
		SqlIdColumns ids,
		ColumnDateRange rawValidityDate,
		ColumnDateRange validityDate,
		List<ConnectorSqlSelects> sqlSelects,
		List<SqlFilters> sqlFilters,
		Optional<QueryStep> stratificationTable
) {

	public PreprocessingCteInput {
		Objects.requireNonNull(sourceTable, "sourceTable");
		Objects.requireNonNull(ids, "ids");
		Objects.requireNonNull(rawValidityDate, "rawValidityDate");
		Objects.requireNonNull(validityDate, "validityDate");
		sqlSelects = List.copyOf(sqlSelects);
		sqlFilters = List.copyOf(sqlFilters);
		Objects.requireNonNull(stratificationTable, "stratificationTable");
	}
}
