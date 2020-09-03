package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;

public class FloatStore extends ColumnStoreAdapter<FloatStore> {

	private final float[] values;

	public FloatStore(ImportColumn column, float[] values) {
		super(column);
		this.values = values;
	}

	@Override
	public FloatStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(FloatStore.class::cast).mapToInt(store -> store.values.length).sum();
		final float[] mergedValues = new float[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final FloatStore doubleStore = (FloatStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new FloatStore(getColumn(), mergedValues);
	}

	@Override
	public boolean has(int event) {
		return !Float.isNaN(values[event]);
	}

	@Override
	public double getReal(int event) {
		return values[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getReal(event);
	}
}
