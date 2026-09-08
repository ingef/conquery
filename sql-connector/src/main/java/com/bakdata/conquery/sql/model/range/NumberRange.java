package com.bakdata.conquery.sql.model.range;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.bakdata.conquery.models.common.InclusiveRange;
import jakarta.validation.constraints.NotNull;

/** Inclusive numeric range. An empty bound represents an unbounded side. */
public record NumberRange(
		@NotNull Optional<BigDecimal> minimum,
		@NotNull Optional<BigDecimal> maximum
) implements InclusiveRange<BigDecimal> {

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
