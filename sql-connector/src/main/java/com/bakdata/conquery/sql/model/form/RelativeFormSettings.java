package com.bakdata.conquery.sql.model.form;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;

public record RelativeFormSettings(
		@NotNull FormIndexSelector indexSelector,
		@NotNull FormIndexPlacement indexPlacement,
		@PositiveOrZero int timeCountBefore,
		@PositiveOrZero int timeCountAfter,
		@NotNull FormCalendarUnit timeUnit,
		@NotEmpty List<@NotNull @Valid ResolutionAndAlignment> resolutions
) {
	public RelativeFormSettings { resolutions = ModelNormalization.immutableCopy(resolutions); }
}
