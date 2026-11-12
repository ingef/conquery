package com.bakdata.conquery.sql.model.range;

import java.time.LocalDate;
import java.util.Optional;

import com.bakdata.conquery.models.common.InclusiveRange;
import jakarta.validation.constraints.NotNull;

/** Inclusive date restriction. An empty bound represents an unbounded side. */
public record DateRange(
		@NotNull Optional<LocalDate> minimum,
		@NotNull Optional<LocalDate> maximum
) implements InclusiveRange<LocalDate> {

	public static DateRange closed(LocalDate minimum, LocalDate maximum) {
		return new DateRange(Optional.of(minimum), Optional.of(maximum));
	}
}
