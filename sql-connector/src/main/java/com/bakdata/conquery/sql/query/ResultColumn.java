package com.bakdata.conquery.sql.query;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Ordered output-column metadata, with presentation names already resolved for the query locale. */
public record ResultColumn(
		String label,
		Optional<String> defaultLabel,
		Optional<String> description,
		ResultType type,
		Set<String> semantics
) {

	public ResultColumn {
		label = ModelValidation.requireNonBlank(label, "label");
		defaultLabel = Objects.requireNonNull(defaultLabel, "defaultLabel")
				.map(value -> ModelValidation.requireNonBlank(value, "defaultLabel"));
		Objects.requireNonNull(description, "description");
		Objects.requireNonNull(type, "type");
		semantics = Set.copyOf(Objects.requireNonNull(semantics, "semantics"));
	}
}
