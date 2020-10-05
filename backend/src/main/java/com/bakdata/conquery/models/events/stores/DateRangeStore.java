package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DATE_RANGES", base = ColumnStore.class)
@Getter
public class DateRangeStore extends ColumnStoreAdapter<DateRangeStore> {

	private CDateRange[] ranges;

	@JsonCreator
	public DateRangeStore(CDateRange[] ranges) {
		this.ranges = ranges;
	}

	@Override
	public boolean has(int event) {
		return ranges[event] != null;
	}

	@Override
	public DateRangeStore merge(List<? extends DateRangeStore> stores) {

		final int newSize = stores.stream().mapToInt(store -> store.getRanges().length).sum();
		final CDateRange[] mergedValues = new CDateRange[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final DateRangeStore doubleStore = (DateRangeStore) store;

			System.arraycopy(doubleStore.getRanges(), 0, mergedValues, start, doubleStore.getRanges().length);
			start += doubleStore.getRanges().length;
		}


		return new DateRangeStore(mergedValues);
	}

	@Override
	public CDateRange getDateRange(int event) {
		return ranges[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getDateRange(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
