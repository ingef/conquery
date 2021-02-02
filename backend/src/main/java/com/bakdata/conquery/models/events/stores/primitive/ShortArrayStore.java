package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;

/**
 * Stores values as Shorts. Can only store 2^16-1 values as MAX is used as NULL value.
 * @apiNote do not instantiate this directly, but use {@link com.bakdata.conquery.models.events.parser.specific.IntegerParser}
 */
@CPSType(id = "SHORTS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class ShortArrayStore implements IntegerStore {

	private final short nullValue;
	private final short[] values;

	@Override
	public int getLines() {
		return values.length;
	}

	@JsonCreator
	public ShortArrayStore(short[] values, short nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public long estimateEventBits() {
		return Short.SIZE;
	}

	public ShortArrayStore select(int[] starts, int[] ends) {
		return new ShortArrayStore(ColumnStore.selectArray(starts, ends, values, short[]::new), nullValue);
	}

	public static ShortArrayStore create(int size) {
		return new ShortArrayStore(new short[size], Short.MAX_VALUE);
	}

	@Override
	public void set(int event, Object value) {
		if (value == null) {
			values[event] = nullValue;
			return;
		}

		values[event] = ((Number) value).shortValue();
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public Long get(int event) {
		return getInteger(event);
	}

	@Override
	public long getInteger(int event) {
		return values[event];
	}

}
