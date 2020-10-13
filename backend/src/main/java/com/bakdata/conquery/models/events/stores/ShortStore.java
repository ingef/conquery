package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "SHORTS", base = ColumnStore.class)
@Getter
public class ShortStore extends ColumnStoreAdapter<Long, ShortStore> {

	private final short nullValue;
	private final short[] values;

	@JsonCreator
	public ShortStore(short[] values, short nullValue) {
		this.nullValue = nullValue;
		this.values = values;
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
