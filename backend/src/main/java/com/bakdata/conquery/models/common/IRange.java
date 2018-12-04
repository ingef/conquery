package com.bakdata.conquery.models.common;

import lombok.NonNull;

public interface IRange<VALUE, CLASS extends IRange<VALUE, CLASS>> {

	VALUE getMin();

	VALUE getMax();

	boolean contains(VALUE value);

	boolean contains(CLASS other);

	CLASS span(@NonNull CLASS other);

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
}
