package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;

public class LongStore extends ColumnStoreAdapter<LongStore> {

	private final long nullValue;
	private final long[] values;

	public LongStore(ImportColumn column, long[] values, long nullValue) {
		super(column);
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public LongStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(LongStore.class::cast).mapToInt(store -> store.values.length).sum();
		final long[] mergedValues = new long[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final LongStore doubleStore = (LongStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new LongStore(getColumn(), mergedValues, nullValue);
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public long getInteger(int event) {
		return values[event];
	}

	@Override
	public long getMoney(int event) {
		return getInteger(event);
	}

	@Override
	public Object getAsObject(int event) {
		return getInteger(event);
	}
}
