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
	private int size = 0;

	public boolean isNull(int event) {
		return nulls.get(event);
	}

	public abstract T get(int event);

	public int add(T value) {
		int event = size++;

		if (value == null) {
			nulls.set(event);
			write(event, nullValue);
		}
		else {
			write(event, value);
		}

		return event;
	}

	protected abstract void write(int position, T obj);

	public void close() {

	}
}
