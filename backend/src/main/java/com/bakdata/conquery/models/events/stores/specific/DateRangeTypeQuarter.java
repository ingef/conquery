package com.bakdata.conquery.models.events.stores.specific;

import java.time.LocalDate;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * a type to store effectively a date range by only storing the first epoch day of the quarter
 **/
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_QUARTER")
@Getter
public class DateRangeTypeQuarter extends DateRangeStore {

	private final IntegerStore store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeQuarter(IntegerStore store) {
		this.store = store;
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public int getLines() {
		return store.getLines();
	}

	@Override
	public CDateRange createScriptValue(CDateRange value) {
		return value;
	}

	public DateRangeTypeQuarter doSelect(int[] starts, int[] ends) {
		return new DateRangeTypeQuarter(store.select(starts, ends));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			store.set(event, null);
		}
		else if (value.hasLowerBound()) {
			store.set(event, (long) value.getMinValue());
		}
		else {
			throw new IllegalArgumentException("Cannot store open dates in QuarterStore");
		}
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public CDateRange get(int event) {
		return getDateRange(event);
	}

	@Override
	public CDateRange getDateRange(int event) {
		final int begin = (int) store.getInteger(event);
		final LocalDate end = QuarterUtils.getLastDayOfQuarter(begin);

		return CDateRange.of(begin, CDate.ofLocalDate(end));
	}
}