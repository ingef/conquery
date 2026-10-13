package com.bakdata.conquery.sql.model.range;

import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Zero-based substring bounds with an exclusive, optionally unbounded end. */
public record SubstringRange(
		@PositiveOrZero int startInclusive,
		@NotNull Optional<@PositiveOrZero Integer> endExclusive
) {

	@AssertTrue(message = "endExclusive must not be smaller than startInclusive")
	public boolean isOrdered() {
		return endExclusive == null || endExclusive.isEmpty() || endExclusive.get() >= startInclusive;
	}

	public static SubstringRange from(int startInclusive) {
		return new SubstringRange(startInclusive, Optional.empty());
	}

	public static SubstringRange between(int startInclusive, int endExclusive) {
		return new SubstringRange(startInclusive, Optional.of(endExclusive));
	}
}
