package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "INTEGERS", base = ColumnStore.class)
@Getter
public class IntegerStore extends ColumnStoreAdapter<IntegerStore> {

	private final int nullValue;
	private final int[] values;

	@JsonCreator
	public IntegerStore(int[] values, int nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public IntegerStore merge(List<? extends IntegerStore> stores) {

		final int newSize = stores.stream().map(IntegerStore.class::cast).mapToInt(store -> store.values.length).sum();
		final int[] mergedValues = new int[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final IntegerStore doubleStore = (IntegerStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new IntegerStore(mergedValues, nullValue);
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
