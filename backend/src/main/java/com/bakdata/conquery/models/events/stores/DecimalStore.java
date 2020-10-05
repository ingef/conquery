package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DECIMALS", base = ColumnStore.class)
@Getter
public class DecimalStore extends ColumnStoreAdapter<DecimalStore> {

	private final BigDecimal[] values;

	@JsonCreator
	public DecimalStore(BigDecimal[] values) {
		this.values = values;
	}

	@Override
	public boolean has(int event) {
		return values[event] != null;
	}

	@Override
	public DecimalStore merge(List<? extends DecimalStore> stores) {
		final int newSize = stores.stream().map(DecimalStore.class::cast).mapToInt(store -> store.values.length).sum();
		final BigDecimal[] mergedValues = new BigDecimal[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final DecimalStore doubleStore = (DecimalStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new DecimalStore(mergedValues);
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return values[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getDecimal(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
