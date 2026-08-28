package com.bakdata.conquery.sql.model.schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Physical source used to enumerate all entities, for example when compiling a root-level negation. */
public record EntitySchema(
		@NotNull @Valid SqlTable table,
		@NotNull @Valid ResolvedColumn primaryId
) {

	@AssertTrue(message = "primaryId must belong to the entity table")
	public boolean isPrimaryIdOnEntityTable() {
		return table == null || primaryId == null || table.equals(primaryId.table());
	}
}
