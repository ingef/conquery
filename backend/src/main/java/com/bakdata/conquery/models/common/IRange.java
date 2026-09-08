package com.bakdata.conquery.models.common;

import java.util.Optional;

import lombok.NonNull;

public interface IRange<VALUE extends Comparable<? super VALUE>, CLASS extends IRange<VALUE, CLASS>> extends InclusiveRange<VALUE> {

	VALUE getMin();

	VALUE getMax();

	@Override
	default Optional<VALUE> minimum() {
		return Optional.ofNullable(getMin());
	}

	@Override
	default Optional<VALUE> maximum() {
		return Optional.ofNullable(getMax());
	}

	boolean contains(VALUE value);

	boolean contains(CLASS other);

	CLASS span(@NonNull CLASS other);

	default com.google.common.collect.Range<VALUE> toGuavaRange() {
		if(isAtLeast()) {
			return com.google.common.collect.Range.atLeast(getMin());
		}
		if(isAtMost()) {
			return com.google.common.collect.Range.atMost(getMax());
		}
		if(isAll()) {
			return com.google.common.collect.Range.all();
		}
		return com.google.common.collect.Range.closed(getMin(), getMax());
	}

	default boolean intersects(CLASS other) {
		if(other == null) {
			return false;
		}

		return other.contains(this.getMin())
				|| other.contains(this.getMax())
				|| this.contains(other.getMin())
				|| this.contains(this.getMax());
	}

	default boolean isOpen() {
		return getMin() == null || getMax() == null;
	}

	default boolean isAll() {
		return getMin() == null && getMax() == null;
	}

	default boolean isAtMost() {
		return isOpen() && getMax() != null;
	}

	default boolean isAtLeast() {
		return isOpen() && getMin() != null;
	}

	default boolean isExactly() {
		return getMin() != null && getMax() == getMin();
	}

	default boolean hasLowerBound() {
		return getMin() != null;
	}

	default boolean hasUpperBound() {
		return getMax() != null;
	}
}
