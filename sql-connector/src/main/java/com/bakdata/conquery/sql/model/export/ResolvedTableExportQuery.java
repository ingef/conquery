package com.bakdata.conquery.sql.model.export;

import java.util.List;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.model.result.ResultColumn;

/** Fully resolved input for exporting individual physical rows. */
public record ResolvedTableExportQuery(
		ResolvedQuery prerequisite,
		DateRange dateRestriction,
		List<ResolvedExportTable> tables,
		List<ResultColumn> resultColumns
) {
	public ResolvedTableExportQuery {
		tables = List.copyOf(tables);
		resultColumns = List.copyOf(resultColumns);
	}
}
