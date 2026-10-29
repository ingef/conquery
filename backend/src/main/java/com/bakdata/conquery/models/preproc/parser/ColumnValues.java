package com.bakdata.conquery.models.preproc.parser;

import java.util.BitSet;

/**
 * For Preprocessing: per Column Store to encode null in auxiliary bitset, allowing primitive storage.
 */
public abstract class ColumnValues<T> {

	private final BitSet nulls = new BitSet();

	public boolean isNull(int event) {
		return nulls.get(event);
	}

	public abstract T get(int event);

	public final int addNull() {
		final int event = prepareAdd(true);
		appendNull();
		return event;
	}

	protected final int prepareAdd(boolean isNull) {
		final int event = size();
		if (isNull) {
			nulls.set(event);
		}
		return event;
	}

	protected abstract void appendNull();

	protected abstract void truncateValues(int size);

	protected abstract int size();

	public final int getSize() {
		return size();
	}

	public final void truncate(int size) {
		final int currentSize = size();
		if (size < 0 || size > currentSize) {
			throw new IllegalArgumentException("Cannot truncate column values from %d to %d".formatted(currentSize, size));
		}

		truncateValues(size);
		if (size < nulls.length()) {
			nulls.clear(size, nulls.length());
		}
	}

}
