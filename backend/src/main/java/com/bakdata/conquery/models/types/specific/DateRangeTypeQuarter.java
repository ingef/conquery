package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.date.QuarterDateStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

/**
 a type to store effectively a date range by only storing the first epoch day of the quarter 
 **/
@CPSType(base=CType.class, id="DATE_RANGE_QUARTER")
public class DateRangeTypeQuarter extends CType<CDateRange, Integer> {
	public DateRangeTypeQuarter() {
		super(MajorTypeId.DATE_RANGE);
	}

	@Override
	public ColumnStore createStore(int size) {
		return QuarterDateStore.create(size);
	}

	@Override
	public CDateRange createScriptValue(Integer value) {
		return CQuarter.toRange(value);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
	
	@Override
		public Object createPrintValue(Integer value) {
			return createScriptValue(value).toString();
		}
}