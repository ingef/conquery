package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.date.DateRangeStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = CType.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
public class DateRangeTypeDateRange extends CType<CDateRange, CDateRange> {

	public DateRangeTypeDateRange() {
		super(MajorTypeId.DATE_RANGE);
	}

	@Override
	public ColumnStore createStore(int size) {
		return DateRangeStore.create(size);
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}

	@Override
	public long estimateMemoryBitWidth() {
		return 128 + Long.SIZE;
	}

	@Override
	public ColumnStore<CDateRange> select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, CDateRange value) {

	}

	@Override
	public CDateRange get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}