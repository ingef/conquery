package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


/**
 * Store values as ints, can only store 2^32-1 values, as MAX is used as NULL signifier.
 *
 * @apiNote do not instantiate this directly, but use {@link com.bakdata.conquery.models.preproc.parser.specific.IntegerParser}
 */
@CPSType(id = "INTEGERS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class IntArrayStore implements IntegerStore {

	private final int nullValue;
	private final int[] values;

	@JsonCreator
	public IntArrayStore(int[] values, int nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	public static IntArrayStore create(int size) {
		return new IntArrayStore(new int[size], Integer.MAX_VALUE);
	}

	@Override
	@ToString.Include
	public int getLines() {
		return values.length;
	}

	@Override
	public long estimateEventBits() {
		return Integer.SIZE;
	}

	public IntArrayStore select(int[] starts, int[] ends) {
		return new IntArrayStore(ColumnStore.selectArray(starts, ends, values, int[]::new), nullValue);
	}


	@Override
	public void setNull(int event) {
		values[event] = nullValue;
	}

	@Override
	public void setInteger(int event, long value) {
		values[event] = (int) value;
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public long getInteger(int event) {
		return values[event];
	}
}
