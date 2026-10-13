package com.bakdata.conquery.sql.model.form;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.model.range.DateRange;

public sealed interface ResolvedFormMode {
	record Absolute(DateRange bounds, List<ResolutionAndAlignment> resolutions) implements ResolvedFormMode {
		public Absolute { resolutions = List.copyOf(resolutions); }
	}
	record Relative(RelativeFormSettings settings) implements ResolvedFormMode {}
	record EntityDate(Optional<DateRange> bounds, List<ResolutionAndAlignment> resolutions) implements ResolvedFormMode {
		public EntityDate { resolutions = List.copyOf(resolutions); }
	}
}
