package com.bakdata.conquery.models.events.stores;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LongStore extends ColumnStoreAdapter {

	private final long nullValue;
	private final long[] values;

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public long getInteger(int event) {
		return values[event];
	}
}
