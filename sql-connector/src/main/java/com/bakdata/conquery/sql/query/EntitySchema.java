package com.bakdata.conquery.sql.query;

import java.util.Objects;

/** Physical source used to enumerate all entities, for example when compiling a root-level negation. */
public record EntitySchema(SqlTable table, ResolvedColumn primaryId) {

	public EntitySchema {
		table = Objects.requireNonNull(table, "table");
		primaryId = Objects.requireNonNull(primaryId, "primaryId");
		if (!table.equals(primaryId.table())) {
			throw new IllegalArgumentException("primaryId must belong to the entity table");
		}
	}
}
