package com.bakdata.conquery.sql.query.range;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Inclusive date restriction. An empty bound represents an unbounded side. */
public record DateRange(
		@NotNull Optional<LocalDate> startInclusive,
		@NotNull Optional<LocalDate> endInclusive
) {

	@AssertTrue(message = "startInclusive must not be after endInclusive")
	public boolean isOrdered() {
		return startInclusive == null || endInclusive == null
				|| startInclusive.isEmpty() || endInclusive.isEmpty()
				|| !startInclusive.get().isAfter(endInclusive.get());
	}

	public static DateRange closed(LocalDate startInclusive, LocalDate endInclusive) {
		return new DateRange(Optional.of(startInclusive), Optional.of(endInclusive));
	}
}
