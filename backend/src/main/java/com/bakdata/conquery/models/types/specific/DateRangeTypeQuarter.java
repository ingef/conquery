package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;

/**
 a type to store effectively a date range by only storing the first epoch day of the quarter 
 **/
@CPSType(base=CType.class, id="DATE_RANGE_QUARTER")
public class DateRangeTypeQuarter extends MinorCType<Integer, DateRangeType> {
	public DateRangeTypeQuarter() {
		super(MajorTypeId.DATE_RANGE, int.class);
	}

	@Override
	public CDateRange createScriptValue(Integer value) {
		return CQuarter.toRange(value);
	}
	
	@Override
	public Integer transformFromMajorType(DateRangeType majorType, Object value) {
		return ((CDateRange)value).getMinValue();
	}
	
	@Override
	public CDateRange transformToMajorType(Integer value, DateRangeType majorType) {
		return CQuarter.toRange(value);
	}
	
	@Override
	public boolean canStoreNull() {
		return false;
	}
}