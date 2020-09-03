package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_RANGE_DATE_RANGE") @Getter @Setter
public class DateRangeTypeDateRange extends CType<CDateRange, CDateRange> {

	public DateRangeTypeDateRange() {
		super(MajorTypeId.DATE_RANGE, CDateRange.class);
	}

	@Override
	public ColumnStore createStore(ImportColumn column, Object[] objects) {
		return null; // TODO
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value;
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return 128+Long.SIZE;
	}
}