package com.bakdata.conquery.sql.model.result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Backend-independent identity and logical type of a requested result column.
 *
 * <p>Labels, descriptions, semantics, and formatting instructions are deliberately excluded. Backends retain that
 * presentation metadata and correlate it with compiled columns through {@link #outputId()}.</p>
 */
public record ResultColumn(
		@NotBlank String outputId,
		@NotNull @Valid ResultType type
) {
}
