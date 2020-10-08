package com.bakdata.conquery.models.events.stores;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DATE_RANGES", base = ColumnStore.class)
@Getter
public class DateRangeStore extends ColumnStoreAdapter<CDateRange, DateRangeStore> {

	private final CDateRange[] ranges;

	@JsonCreator
	public DateRangeStore(CDateRange[] ranges) {
		this.ranges = ranges;
	}

	@Override
	public boolean has(int event) {
		return ranges[event] != null;
	}

	@Override
	public DateRangeStore merge(List<DateRangeStore> stores) {

		final int newSize = stores.stream().mapToInt(store -> store.getRanges().length).sum();
		final CDateRange[] mergedValues = new CDateRange[newSize];

		int start = 0;

		for (DateRangeStore store : stores) {
			System.arraycopy(store.getRanges(), 0, mergedValues, start, store.getRanges().length);
			start += store.getRanges().length;
		}


		return new DateRangeStore(mergedValues);
	}

	@Override
	public CDateRange get(int event) {
		return ranges[event];
	}

}
