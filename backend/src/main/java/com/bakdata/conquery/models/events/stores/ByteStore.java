package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;


@CPSType(id = "BYTES", base = ColumnStore.class)
@Getter
public class ByteStore extends ColumnStoreAdapter<Byte, ByteStore> {

	private final byte nullValue;
	private final byte[] values;

	@JsonCreator
	public ByteStore(byte[] values, byte nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public ByteStore merge(List<ByteStore> stores) {


		final int newSize = stores.stream().map(ByteStore.class::cast).mapToInt(store -> store.values.length).sum();
		final byte[] mergedValues = new byte[newSize];

		int start = 0;

		for (ByteStore store : stores) {

			System.arraycopy(store.values, 0, mergedValues, start, store.values.length);
			start += store.values.length;
		}

		return new ByteStore(mergedValues, nullValue);
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public Byte get(int event) {
		return values[event];
	}
}
