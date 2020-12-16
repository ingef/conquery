package com.bakdata.conquery.models.events.stores.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DateStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
public class DateRangeTypeDateRange extends ColumnStore<CDateRange> {

	private final DateStore minStore;
	private final DateStore maxStore;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeDateRange(DateStore minStore, DateStore maxStore) {
		this.minStore = minStore;
		this.maxStore = maxStore;
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}

	@Override
	public long estimateEventBits() {
		return minStore.estimateEventBits() * 2;
	}

	@Override
	public DateRangeTypeDateRange select(int[] starts, int[] length) {
		return new DateRangeTypeDateRange(minStore.select(starts, length), maxStore.select(starts, length));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			minStore.set(event, null);
			maxStore.set(event, null);
			return;
		}

		if (value.hasLowerBound()) {
			minStore.set(event, value.getMinValue());
		}
		else {
			minStore.set(event, null);
		}

		if (value.hasUpperBound()) {
			maxStore.set(event, value.getMaxValue());
		}
		else {
			maxStore.set(event, null);
		}
	}

	@Override
	public CDateRange get(int event) {
		return getDateRange(event);
	}

	@Override
	public CDateRange getDateRange(int event) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (minStore.has(event)) {
			min = minStore.getDate(event);
		}

		if (maxStore.has(event)) {
			max = maxStore.getDate(event);
		}

		return CDateRange.of(min, max);
	}

	@Override
	public boolean has(int event) {
		return minStore.has(event) || maxStore.has(event);
	}
}