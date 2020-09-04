package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;


public class ByteStore extends ColumnStoreAdapter<ByteStore> {

	private final byte nullValue;
	private final byte[] values;

	public ByteStore(ImportColumn column, byte[] values, byte nullValue) {
		super(column);
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public ByteStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(ByteStore.class::cast).mapToInt(store -> store.values.length).sum();
		final byte[] mergedValues = new byte[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final ByteStore doubleStore = (ByteStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new ByteStore(getColumn(), mergedValues, nullValue);
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
