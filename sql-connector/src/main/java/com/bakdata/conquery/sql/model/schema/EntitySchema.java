package com.bakdata.conquery.sql.model.schema;

import com.bakdata.conquery.models.datasets.ColumnType;
import com.bakdata.conquery.sql.validation.AllowedColumnTypes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Physical source used to enumerate all entities, for example when compiling a root-level negation. */
public record EntitySchema(
		@NotNull @Valid @AllowedColumnTypes(ColumnType.STRING) ResolvedColumn primaryId
) {

	/** Physical table containing all entities. */
	public SqlTable table() {
		return primaryId == null ? null : primaryId.table();
	}
}
