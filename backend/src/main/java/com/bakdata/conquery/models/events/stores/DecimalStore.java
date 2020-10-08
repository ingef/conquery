package com.bakdata.conquery.models.events.stores;

import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DECIMALS", base = ColumnStore.class)
@Getter
public class DecimalStore extends ColumnStoreAdapter<BigDecimal, DecimalStore> {

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
	public DecimalStore merge(List<DecimalStore> stores) {
		final int newSize = stores.stream().map(DecimalStore.class::cast).mapToInt(store -> store.values.length).sum();
		final BigDecimal[] mergedValues = new BigDecimal[newSize];

		int start = 0;

		for (DecimalStore store : stores) {
			System.arraycopy(store.values, 0, mergedValues, start, store.values.length);
			start += store.values.length;
		}

		return new DecimalStore(mergedValues);
	}

	@Override
	public BigDecimal get(int event) {
		return values[event];
	}


}
