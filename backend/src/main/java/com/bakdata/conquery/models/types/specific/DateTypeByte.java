package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE_INT8") @Getter @Setter
public class DateTypeByte extends MinorCType<Byte, DateType> {
	
	private byte maxValue;
	private byte minValue;
	
	public DateTypeByte() {
		super(MajorTypeId.DATE, byte.class);
	}
	
	@JsonCreator
	public DateTypeByte(long lines, long nullLines, byte maxValue, byte minValue) {
		this();
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.maxValue = maxValue;
		this.minValue = minValue;
	}
	
	@Override
	public Integer transformToMajorType(Byte value, DateType majorType) {
		return (int)value;
	}
	
	@Override
	public Byte transformFromMajorType(DateType majorType, Object value) {
		int v = (Integer) value;
		if(v>Byte.MAX_VALUE || v<Byte.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (byte) v;
	}

	@Override
	public Object createScriptValue(Byte value) {
		return CDate.toLocalDate(transformToMajorType(value, null));
	}

	@Override
	public Object createPrintValue(Byte value) {
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