package com.bakdata.conquery.models.events.parser;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.roaringbitmap.RoaringBitmap;

/**
 * per Column Store to encode null in auxiliary bitset, allowing primitive storage.
 */
@SuppressWarnings("Unchecked")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ColumnValues<T> {

	private final T nullValue;
	private final RoaringBitmap nulls = new RoaringBitmap();

	public boolean isNull(int event) {
		return nulls.contains(event);
	}

	public final T get(int event) {
		return read(event);
	}

	protected abstract T read(int event);

	public int add(T value) {
		int event = size();

		if (value == null) {
			nulls.add(event);
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
		return nulls.getCardinality();
	}

	protected abstract int countValues();

	public void close() {

	}
}
