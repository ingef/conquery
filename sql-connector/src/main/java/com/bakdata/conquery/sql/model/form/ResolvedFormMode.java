package com.bakdata.conquery.sql.model.form;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.sql.model.internal.ModelNormalization;
import com.bakdata.conquery.sql.model.range.DateRange;

public sealed interface ResolvedFormMode {
	record Absolute(
			@NotNull @Valid DateRange bounds,
			@NotEmpty List<@NotNull @Valid ResolutionAndAlignment> resolutions
	) implements ResolvedFormMode {
		public Absolute { resolutions = ModelNormalization.immutableCopy(resolutions); }
	}
	record Relative(@NotNull @Valid RelativeFormSettings settings) implements ResolvedFormMode {}
	record EntityDate(
			@NotNull Optional<@Valid DateRange> bounds,
			@NotEmpty List<@NotNull @Valid ResolutionAndAlignment> resolutions
	) implements ResolvedFormMode {
		public EntityDate { resolutions = ModelNormalization.immutableCopy(resolutions); }
	}
}
