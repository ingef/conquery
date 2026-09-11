package com.bakdata.conquery.sql.model.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.range.DateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** One resolved external entity together with its validity ranges and ordered result values. */
public record ExternalEntity(
		@NotBlank String entityId,
		@NotNull List<@NotNull @Valid DateRange> validityDates,
		@NotNull Map<@NotBlank String, @NotNull List<@NotNull String>> values
) {

	public ExternalEntity {
		validityDates = ModelNormalization.immutableCopy(validityDates);
		values = deepImmutableCopy(values);
	}

	private static Map<String, List<String>> deepImmutableCopy(Map<String, List<String>> values) {
		if (values == null) {
			return null;
		}
		Map<String, List<String>> copy = new LinkedHashMap<>();
		values.forEach((column, columnValues) -> copy.put(column, ModelNormalization.immutableCopy(columnValues)));
		return Collections.unmodifiableMap(copy);
	}
}
