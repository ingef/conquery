package com.bakdata.conquery.models.types.specific.daterange;

import java.time.LocalDate;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * a type to store effectively a date range by only storing the first epoch day of the quarter
 **/
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_QUARTER")
@Getter
public class DateRangeTypeQuarter extends CType<CDateRange> {

	private final CType<Long> store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeQuarter(CType<Long> store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
	}

	@Override
	public long estimateMemoryFieldSize() {
		return store.estimateMemoryFieldSize();
	}

	@Override
	public CDateRange createScriptValue(CDateRange value) {
		return value;
	}

	public DateRangeTypeQuarter select(int[] starts, int[] ends) {
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
		final int begin = (int) store.getInteger(event);
		final LocalDate end = QuarterUtils.getLastDayOfQuarter(begin);

		return CDateRange.of(begin, CDate.ofLocalDate(end));
	}
}