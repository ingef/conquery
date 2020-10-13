package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "INTEGERS", base = ColumnStore.class)
@Getter
public class IntegerStore extends ColumnStoreAdapter<Long, IntegerStore> {

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

	public IntegerStore select(int[] starts, int[] ends) {
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
		return (long) values[event];
	}
}
