package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.util.BitSet;
import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;


public class BooleanStore extends ColumnStoreAdapter<BooleanStore> {

	private final BitSet values;

	public BooleanStore(ImportColumn column, BitSet values) {
		super(column);
		this.values = values;
	}

	@Override
	public BooleanStore merge(List<? extends ColumnStore<?>> stores) {
		if(!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))){
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(BooleanStore.class::cast).mapToInt(store -> store.values.size()).sum();
		final BitSet mergedValues = new BitSet(newSize);

		int start = 0;

		//TODO !

		return new BooleanStore(getColumn(),mergedValues);
	}

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public boolean getBoolean(int event) {
		return values.get(event);
	}

	@Override
	public Object getAsObject(int event) {
		return getBoolean(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}
}
