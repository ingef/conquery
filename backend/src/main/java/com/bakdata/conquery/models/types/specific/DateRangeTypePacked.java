package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_RANGE_2UINT16") @Getter @Setter
public class DateRangeTypePacked extends MinorCType<Integer, DateRangeType> {

	@JsonIgnore
	private transient boolean onlyQuarters = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	public DateRangeTypePacked() {
		super(MajorTypeId.DATE_RANGE, int.class);
	}
	
	@Override
	public CDateRange transformToMajorType(Integer value, DateRangeType majorType) {
		return CDateRange.of(
			PackedUnsigned1616.getLeft(value)+minValue,
			PackedUnsigned1616.getRight(value)+minValue
		);
	}
	
	@Override
	public Integer transformFromMajorType(DateRangeType majorType, Object value) {
		CDateRange v = (CDateRange) value;
		if(v.getMaxValue()>Integer.MAX_VALUE || v.getMinValue()<Integer.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return PackedUnsigned1616.pack(v.getMinValue()-minValue, v.getMaxValue()-minValue);
	}

	@Override
	public Object createPrintValue(Integer value) {
		if (value == null) {
			return "";
		}

		return transformToMajorType(value, null);
	}
}