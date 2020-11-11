package com.bakdata.conquery.models.types.specific.daterange;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.date.DateRangeStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = ColumnStore.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
public class DateRangeTypeDateRange extends CType<CDateRange, CDateRange> {

	private final DateRangeStore store;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeDateRange(DateRangeStore store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
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
	public DateRangeTypeDateRange select(int[] starts, int[] length) {
		return new DateRangeTypeDateRange(store.select(starts, length));
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