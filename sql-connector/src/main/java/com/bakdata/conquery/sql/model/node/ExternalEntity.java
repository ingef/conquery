package com.bakdata.conquery.sql.model.node;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.range.DateRange;

/**
 * One resolved external entity together with its validity ranges and ordered result values.
 * @implSpec this model is an exception because {@code values} is not defensively copied, due to its possible size.
 */
public record ExternalEntity(
		@NotBlank String entityId,
		@NotNull List<@NotNull @Valid DateRange> validityDates,
		@NotNull Map<@NotBlank String, @NotNull List<@NotNull String>> values
) {

	public ExternalEntity {
		validityDates = ModelNormalization.immutableCopy(validityDates);
	}

}
