package com.bakdata.conquery.sql.translate.query.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import lombok.NonNull;
import org.jooq.Record;
import org.jooq.Table;

/** Fully SQL-resolved input needed to build a connector preprocessing CTE. */
public record PreprocessingCteInput(
		@NonNull Table<Record> sourceTable,
		@NonNull SqlIdColumns ids,
		@NonNull ColumnDateRange rawValidityDate,
		@NonNull ColumnDateRange validityDate,
		List<ConnectorSqlSelects> sqlSelects,
		List<SqlFilters> sqlFilters,
		@NonNull Optional<QueryStep> stratificationTable
) {

	public PreprocessingCteInput {
		sqlSelects = List.copyOf(sqlSelects);
		sqlFilters = List.copyOf(sqlFilters);
	}
}
