package com.bakdata.conquery.sql.model.form;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import lombok.Getter;

public record RelativeFormSettings(
		@Getter @NotNull FormIndexSelector indexSelector,
		@Getter @NotNull FormIndexPlacement indexPlacement,
		@Getter @PositiveOrZero int timeCountBefore,
		@Getter @PositiveOrZero int timeCountAfter,
		@Getter @NotNull FormCalendarUnit timeUnit,
		@NotEmpty List<@NotNull @Valid ResolutionAndAlignment> resolutions
) {
	public RelativeFormSettings { resolutions = ModelNormalization.immutableCopy(resolutions); }
	public List<ResolutionAndAlignment> getResolutionsAndAlignmentMap() { return resolutions; }
}
