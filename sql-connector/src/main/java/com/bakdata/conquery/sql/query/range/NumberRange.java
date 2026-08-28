package com.bakdata.conquery.sql.query.range;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Inclusive numeric range. An empty bound represents an unbounded side. */
public record NumberRange(
		@NotNull Optional<BigDecimal> minimum,
		@NotNull Optional<BigDecimal> maximum
) {

	@AssertTrue(message = "minimum must not be greater than maximum")
	public boolean isOrdered() {
		return minimum == null || maximum == null
				|| minimum.isEmpty() || maximum.isEmpty()
				|| minimum.get().compareTo(maximum.get()) <= 0;
	}

	public static NumberRange closed(Number minimum, Number maximum) {
		return new NumberRange(Optional.of(decimal(minimum)), Optional.of(decimal(maximum)));
	}

	public static NumberRange atLeast(Number minimum) {
		return new NumberRange(Optional.of(decimal(minimum)), Optional.empty());
	}

	public static NumberRange atMost(Number maximum) {
		return new NumberRange(Optional.empty(), Optional.of(decimal(maximum)));
	}

	public static NumberRange unbounded() {
		return new NumberRange(Optional.empty(), Optional.empty());
	}

	private static BigDecimal decimal(Number value) {
		Objects.requireNonNull(value, "value");
		return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
	}
}
