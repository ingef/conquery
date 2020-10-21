package com.bakdata.conquery.models.events.stores.date;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@CPSType(id = "DATE_RANGES", base = ColumnStore.class)
@Getter
public class DateRangeStore extends ColumnStoreAdapter<CDateRange> {

	private final CDateRange[] values;

	@JsonCreator
	public DateRangeStore(CDateRange[] ranges) {
		this.values = ranges;
	}

	public DateRangeStore select(int[] starts, int[] ends) {
		return new DateRangeStore(ColumnStore.selectArray(starts, ends, values, CDateRange[]::new));
	}

	public static DateRangeStore create(int size) {
		return new DateRangeStore(new CDateRange[size]);
	}

	@Override
	public void set(int event, CDateRange value) {
		values[event] = value;
	}

	@Override
	public boolean has(int event) {
		return values[event] != null;
	}

	@Override
	public CDateRange get(int event) {
		return values[event];
	}

}
