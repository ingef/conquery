package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Stores values as longs, can only Store 2^64-1 as MAX is used as NULL marker.
 *
 * @apiNote do not instantiate this directly, but use {@link com.bakdata.conquery.models.preproc.parser.specific.IntegerParser}
 */
@CPSType(id = "LONGS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class LongArrayStore implements IntegerStore {

	private final long nullValue;
	private final long[] values;

	@Override
	@ToString.Include
	public int getLines() {
		return values.length;
	}

	@JsonCreator
	public LongArrayStore(long[] values, long nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	public static LongArrayStore create(int size) {
		return new LongArrayStore(new long[size], Long.MAX_VALUE);
	}

	@Override
	public long estimateEventBits() {
		return Long.SIZE;
	}

	public LongArrayStore select(int[] starts, int[] ends) {
		return new LongArrayStore(ColumnStore.selectArray(starts, ends, values, long[]::new), nullValue);
	}

	@Override
	public LongArrayStore createDescription() {
		return ColumnStore.emptyCopy(this);
	}

	@Override
	public void setNull(int event) {
		values[event] = nullValue;
	}

	@Override
	public void setInteger(int event, long value) {
		values[event] = value;
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
