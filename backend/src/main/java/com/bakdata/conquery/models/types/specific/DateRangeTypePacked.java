package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.PackedDateRangeStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.util.PackedUnsigned1616;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_RANGE_2UINT16") @Getter @Setter
public class DateRangeTypePacked extends CType<Integer, Integer> {

	private int maxValue;
	private int minValue;
	
	public DateRangeTypePacked() {
		super(MajorTypeId.DATE_RANGE, int.class);
	}

	@Override
	public ColumnStore createStore(int size) {
		return PackedDateRangeStore.create(size);
	}

	@Override
	public Object createPrintValue(Integer value) {
		if (value == null) {
			return "";
		}

		return CDateRange.of(
			PackedUnsigned1616.getLeft(value)+minValue,
			PackedUnsigned1616.getRight(value)+minValue
		);
	}

	@Override
	public boolean canStoreNull() {
		return false;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Integer.SIZE;
	}
}