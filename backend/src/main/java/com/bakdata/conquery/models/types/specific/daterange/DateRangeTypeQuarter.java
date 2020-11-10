package com.bakdata.conquery.models.types.specific.daterange;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.date.QuarterDateStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * a type to store effectively a date range by only storing the first epoch day of the quarter
 **/
@CPSType(base = ColumnStore.class, id = "DATE_RANGE_QUARTER")
@Getter
public class DateRangeTypeQuarter extends CType<CDateRange, CDateRange> {

	private final QuarterDateStore store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeQuarter(QuarterDateStore store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
	}

	@Override
	public CDateRange createScriptValue(CDateRange value) {
		return value;
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		return createScriptValue(value).toString();
	}

	@Override
	public DateRangeTypeQuarter select(int[] starts, int[] length) {
		return new DateRangeTypeQuarter(store.select(starts, length));
	}

	@Override
	public void set(int event, CDateRange value) {
		store.set(event, value);
	}

	@Override
	public CDateRange get(int event) {
		return store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}