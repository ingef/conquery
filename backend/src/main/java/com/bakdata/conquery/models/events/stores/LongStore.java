package com.bakdata.conquery.models.events.stores;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "LONGS", base = ColumnStore.class)
@Getter
public class LongStore extends ColumnStoreAdapter<Long, LongStore> {

	private final long nullValue;
	private final long[] values;

	@JsonCreator
	public LongStore(long[] values, long nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	public static LongStore create(int size) {
		return new LongStore(new long[size], Long.MAX_VALUE);
	}


	@Override
	public void set(int event, Long value) {
		if (value == null) {
			values[event] = nullValue;
			return;
		}

		values[event] = value;
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public Long get(int event) {
		return values[event];
	}
}
