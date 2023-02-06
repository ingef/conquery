package com.bakdata.conquery.models.preproc.parser;

import java.util.BitSet;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * For Preprocessing: per Column Store to encode null in auxiliary bitset, allowing primitive storage.
 */
@SuppressWarnings("Unchecked")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ColumnValues<T> {

	private final T nullValue;
	private final BitSet nulls = new BitSet();

	public boolean isNull(int event) {
		return nulls.get(event);
	}

	public abstract T get(int event);

	public final int add(T value) {
		int event = size();

		if (value == null) {
			nulls.set(event);
			append(nullValue);
		}
		else {
			append(value);
		}

		return event;
	}

	protected abstract void append(T obj);

	protected abstract int size();

}
