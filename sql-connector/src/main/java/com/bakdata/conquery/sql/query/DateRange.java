package com.bakdata.conquery.sql.query;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Inclusive date restriction. An empty bound represents an unbounded side. */
public record DateRange(Optional<LocalDate> startInclusive, Optional<LocalDate> endInclusive) {

	public DateRange {
		startInclusive = Objects.requireNonNull(startInclusive, "startInclusive");
		endInclusive = Objects.requireNonNull(endInclusive, "endInclusive");
		if (startInclusive.isPresent() && endInclusive.isPresent()
				&& startInclusive.get().isAfter(endInclusive.get())) {
			throw new IllegalArgumentException("startInclusive must not be after endInclusive");
		}
	}

	public static DateRange closed(LocalDate startInclusive, LocalDate endInclusive) {
		return new DateRange(Optional.of(startInclusive), Optional.of(endInclusive));
	}
}
