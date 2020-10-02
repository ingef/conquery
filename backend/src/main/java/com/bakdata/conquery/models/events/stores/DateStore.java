package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DATES", base = ColumnStore.class)
public class DateStore extends ColumnStoreAdapter<DateStore> {

	private final int nullValue;
	private final int[] values;

	@JsonCreator
	public DateStore(ImportColumn column, int[] values, int nullValue) {
		super(column);
		this.nullValue = nullValue;
		this.values = values;
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public DateStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(DateStore.class::cast).mapToInt(store -> store.values.length).sum();
		final int[] mergedValues = new int[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final DateStore doubleStore = (DateStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new DateStore(getColumn(), mergedValues, nullValue);
	}

	@Override
	public CDateRange getDateRange(int event) {
		return CDateRange.exactly(values[event]);
	}

	@Override
	public Object getAsObject(int event) {
		return CDate.toLocalDate(getDate(event));
	}

	@Override
	public int getDate(int event) {
		return values[event];
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
