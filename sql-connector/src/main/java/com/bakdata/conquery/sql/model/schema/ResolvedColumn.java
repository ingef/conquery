package com.bakdata.conquery.sql.model.schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.models.datasets.ColumnType;

/** A column whose owning table, physical name, and logical type are known. */
public record ResolvedColumn(
		@NotBlank String logicalId,
		@NotNull @Valid SqlTable table,
		@NotBlank String physicalName,
		@NotNull ColumnType type,
		boolean nullable
) {
}
