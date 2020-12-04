package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
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
public class ShortStore extends ColumnStore<Long> {

	private final short nullValue;
	private final short[] values;

	@JsonCreator
	public ShortStore(short[] values, short nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public long estimateEventBits() {
		return Short.SIZE;
	}

	public ShortStore select(int[] starts, int[] ends) {
		return new ShortStore(ColumnStore.selectArray(starts, ends, values, short[]::new), nullValue);
	}

	public static ShortStore create(int size) {
		return new ShortStore(new short[size], Short.MAX_VALUE);
	}

	@Override
	public void set(int event, Long value) {
		if (value == null) {
			values[event] = nullValue;
			return;
		}

		values[event] = value.shortValue();
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public Long get(int event) {
		return (long) values[event];
	}
}
