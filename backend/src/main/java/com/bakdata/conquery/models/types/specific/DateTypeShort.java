package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_INT16") @Getter @Setter
public class DateTypeShort extends MinorCType<Short, DateType> {
	
	private short maxValue;
	private short minValue;
	
	public DateTypeShort() {
		super(MajorTypeId.DATE, short.class);
	}
	
	@JsonCreator
	public DateTypeShort(long lines, long nullLines, short maxValue, short minValue) {
		this();
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.maxValue = maxValue;
		this.minValue = minValue;
	}
	
	@Override
	public Integer transformToMajorType(Short value, DateType majorType) {
		return (int)value;
	}
	
	@Override
	public Short transformFromMajorType(DateType majorType, Object value) {
		int v = (Integer) value;
		if(v>Short.MAX_VALUE || v<Short.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (short) v;
	}

	@Override
	public Object createScriptValue(Short value) {
		return CDate.toLocalDate(transformToMajorType(value, null));
	}

	@Override
	public Object createPrintValue(Short value) {
		if (value == null) {
			return "";
		}

		return CDate.toLocalDate((int)value);
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
}