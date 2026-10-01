package com.bakdata.conquery.models.preproc.parser;

import java.util.BitSet;

import com.bakdata.conquery.models.preproc.OutputRow;
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

	@SuppressWarnings("unchecked")
	public int add(OutputRow outputRow, int outputIndex) {
		return add((T) outputRow.getObject(outputIndex));
	}

	protected final int prepareAdd(boolean isNull) {
		final int event = size();
		if (isNull) {
			nulls.set(event);
		}
		return event;
	}

	protected abstract void append(T obj);

	protected abstract int size();

	public final int getSize() {
		return size();
	}

}
