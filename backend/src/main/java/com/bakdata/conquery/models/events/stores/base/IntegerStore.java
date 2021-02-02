package com.bakdata.conquery.models.events.stores.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


/**
 * Store values as ints, can only store 2^32-1 values, as MAX is used as NULL signifier.
 *
 * @apiNote do not instantiate this directly, but use {@link com.bakdata.conquery.models.events.parser.specific.IntegerParser}
 */
@CPSType(id = "INTEGERS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class IntegerStore extends ColumnStore<Long> {

	private final int nullValue;
	private final int[] values;

	@JsonCreator
	public IntegerStore(int[] values, int nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	public static IntegerStore create(int size) {
		return new IntegerStore(new int[size], Integer.MAX_VALUE);
	}

	@Override
	public long estimateEventBits() {
		return Integer.SIZE;
	}

	public IntegerStore doSelect(int[] starts, int[] ends) {
		return new IntegerStore(ColumnStore.selectArray(starts, ends, values, int[]::new), nullValue);
	}

	@Override
	public void set(int event, Long value) {
		if(value == null) {
			values[event] = nullValue;
			return;
		}

		values[event] = value.intValue();
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
