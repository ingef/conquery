package com.bakdata.conquery.sql.query;

import java.util.List;
import java.util.Objects;

/**
 * Complete input for compiling a concept query to SQL.
 *
 * @param includeValidityDate whether the final result exposes an aggregated validity-date column
 */
public record ResolvedQuery(
		ExecutionTarget target,
		EntitySchema entitySchema,
		QueryNode root,
		boolean includeValidityDate,
		List<ResultColumn> resultColumns
) {

	public ResolvedQuery {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(entitySchema, "entitySchema");
		Objects.requireNonNull(root, "root");
		resultColumns = List.copyOf(Objects.requireNonNull(resultColumns, "resultColumns"));
	}
}
