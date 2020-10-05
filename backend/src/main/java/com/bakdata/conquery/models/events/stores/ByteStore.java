package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "BYTES", base = ColumnStore.class)
@Getter
public class ByteStore extends ColumnStoreAdapter<ByteStore> {

	private final byte nullValue;
	private final byte[] values;

	@JsonCreator
	public ByteStore(byte[] values, byte nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public ByteStore merge(List<? extends ByteStore> stores) {


		final int newSize = stores.stream().map(ByteStore.class::cast).mapToInt(store -> store.values.length).sum();
		final byte[] mergedValues = new byte[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final ByteStore doubleStore = (ByteStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new ByteStore(mergedValues, nullValue);
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
