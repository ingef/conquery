package com.bakdata.conquery.sql.model;

import java.util.List;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.node.QueryNode;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.model.schema.EntitySchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Complete input for compiling a concept query to SQL.
 *
 * @param includeValidityDate whether the final result exposes an aggregated validity-date column
 */
public record ResolvedQuery(
		@NotNull @Valid ExecutionTarget target,
		@NotNull @Valid EntitySchema entitySchema,
		@NotNull @Valid QueryNode root,
		boolean includeValidityDate,
		@NotNull List<@NotNull @Valid ResultColumn> resultColumns
) {

	public ResolvedQuery {
		resultColumns = ModelNormalization.immutableCopy(resultColumns);
	}
}
