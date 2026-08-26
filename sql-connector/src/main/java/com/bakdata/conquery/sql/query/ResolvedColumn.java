package com.bakdata.conquery.sql.query;

import java.util.Objects;

/** A column whose owning table, physical name, and logical type are known. */
public record ResolvedColumn(
		String logicalId,
		SqlTable table,
		String physicalName,
		ColumnType type,
		boolean nullable
) {

	public ResolvedColumn {
		logicalId = ModelValidation.requireNonBlank(logicalId, "logicalId");
		Objects.requireNonNull(table, "table");
		physicalName = ModelValidation.requireNonBlank(physicalName, "physicalName");
		Objects.requireNonNull(type, "type");
	}
}
