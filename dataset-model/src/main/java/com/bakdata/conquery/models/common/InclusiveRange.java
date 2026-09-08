package com.bakdata.conquery.models.common;

import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;

/** A possibly unbounded value range whose present lower and upper bounds are inclusive. */
public interface InclusiveRange<T extends Comparable<? super T>> {

	Optional<T> minimum();

	Optional<T> maximum();

	@AssertTrue(message = "minimum must not be greater than maximum")
	default boolean isOrdered() {
		return minimum() == null || maximum() == null
				|| minimum().isEmpty() || maximum().isEmpty()
				|| minimum().get().compareTo(maximum().get()) <= 0;
	}
}
