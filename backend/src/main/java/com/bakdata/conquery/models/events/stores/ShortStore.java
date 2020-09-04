package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;


public class ShortStore extends ColumnStoreAdapter<ShortStore> {

	private final short nullValue;
	private final short[] values;

	public ShortStore(ImportColumn column, short[] values, short nullValue) {
		super(column);
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public ShortStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(ShortStore.class::cast).mapToInt(store -> store.values.length).sum();
		final short[] mergedValues = new short[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final ShortStore doubleStore = (ShortStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new ShortStore(getColumn(), mergedValues, nullValue);
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public long getInteger(int event) {
		return (long) values[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getInteger(event);
	}
}
