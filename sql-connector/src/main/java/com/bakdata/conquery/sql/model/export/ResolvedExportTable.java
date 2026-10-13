package com.bakdata.conquery.sql.model.export;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import com.bakdata.conquery.sql.model.schema.ResolvedValidityDate;
import com.bakdata.conquery.sql.model.schema.SqlTable;

/** One physical connector table participating in a row-level export. */
public record ResolvedExportTable(
		String conceptName,
		String connectorName,
		SqlTable table,
		ResolvedColumn primaryId,
		ResolvedValidityDate validityDate,
		String source,
		List<Optional<ResolvedColumn>> outputColumns,
		List<ResolvedFilter> filters
) {
	public ResolvedExportTable {
		outputColumns = List.copyOf(outputColumns);
		filters = List.copyOf(filters);
	}
}
