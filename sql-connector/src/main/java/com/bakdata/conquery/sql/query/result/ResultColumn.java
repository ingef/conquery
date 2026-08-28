package com.bakdata.conquery.sql.query.result;

import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.sql.query.internal.ModelNormalization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Ordered output-column metadata, with presentation names already resolved for the query locale. */
public record ResultColumn(
		@NotBlank String label,
		@NotNull Optional<String> defaultLabel,
		@NotNull Optional<String> description,
		@NotNull @Valid ResultType type,
		@NotNull Set<@NotBlank String> semantics
) {

	public ResultColumn {
		semantics = ModelNormalization.immutableCopy(semantics);
	}

	@AssertTrue(message = "defaultLabel must not be blank when present")
	public boolean isDefaultLabelValid() {
		return defaultLabel == null || defaultLabel.isEmpty() || !defaultLabel.get().isBlank();
	}
}
