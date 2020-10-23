package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.date.PackedDateRangeStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=ColumnStore.class, id="DATE_RANGE_2UINT16") @Getter @Setter
public class DateRangeTypePacked extends CType<Integer, CDateRange> {

	private int maxValue;
	private int minValue;

	private final PackedDateRangeStore store;

	public DateRangeTypePacked(PackedDateRangeStore store) {
		super(MajorTypeId.DATE_RANGE);
		this.store = store;
	}

	@Override
	public CDateRange createScriptValue(CDateRange value) {
		if(value == null) {
			return null;
		}
		return value;
	}
	
	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return createScriptValue(value).toString();
	}

	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}

	@Override
	public DateRangeTypePacked select(int[] starts, int[] length) {
		return new DateRangeTypePacked(store.select(starts, length));
	}

	@Override
	public void set(int event, CDateRange value) {
		store.set(event,value);
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