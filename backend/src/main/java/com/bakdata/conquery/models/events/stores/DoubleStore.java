package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;

@CPSType(id = "DOUBLES", base = ColumnStore.class)
public class DoubleStore extends ColumnStoreAdapter<DoubleStore> {

	private final double[] values;

	public DoubleStore(ImportColumn column, double[] values) {
		super(column);
		this.values = values;
	}

	@Override
	public DoubleStore merge(List<? extends ColumnStore<?>> stores) {
		if(!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))){
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(DoubleStore.class::cast).mapToInt(store -> store.values.length).sum();
		final double[] mergedValues = new double[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final DoubleStore doubleStore = (DoubleStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new DoubleStore(getColumn(),mergedValues);
	}

	@Override
	public boolean has(int event) {
		return !Double.isNaN(values[event]);
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
