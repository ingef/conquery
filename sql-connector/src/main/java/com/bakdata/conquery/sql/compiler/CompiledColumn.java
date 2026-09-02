package com.bakdata.conquery.sql.compiler;

import com.bakdata.conquery.sql.model.result.ResultType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Ordered description of one column produced by a {@link CompiledQuery}.
 *
 * @param outputId stable, backend-independent key used to correlate the column with backend metadata
 * @param sqlAlias alias rendered into the SQL statement
 * @param type logical value type expected by backend result decoding
 * @param role role of the column in the result row
 */
public record CompiledColumn(
		@NotBlank String outputId,
		@NotBlank String sqlAlias,
		@NotNull @Valid ResultType type,
		@NotNull ColumnRole role
) {
}
