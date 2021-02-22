package com.bakdata.conquery.models.events.parser;

import java.util.BitSet;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * per Column Store to encode null in auxiliary bitset, allowing primitive storage.
 */
@SuppressWarnings("Unchecked")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ColumnValues<T> {

	private final T nullValue;
	private final BitSet nulls = new BitSet();

	public boolean isNull(int event) {
		return nulls.get(event);
	}

	public final T get(int event) {
		return read(event);
	}

	protected abstract T read(int event);

	public int add(T value) {
		int event = size();

		if (value == null) {
			nulls.set(event);
			write(nullValue);
		}
		else {
			write(value);
		}

		return event;
	}

	public int size() {
		return countValues();
	}

	protected abstract void write(T obj);

	protected int countNulls() {
		return nulls.cardinality();
	}

	protected abstract int countValues();

	public void close() {

	}
}
