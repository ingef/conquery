package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="INTEGER_INT8")
@Getter @Setter
public class IntegerTypeByte extends MinorCType<Byte, IntegerType> {

	private byte maxValue;
	private byte minValue;
	
	public IntegerTypeByte() {
		super(MajorTypeId.INTEGER, byte.class);
	}
	
	@JsonCreator
	public IntegerTypeByte(long lines, long nullLines, byte maxValue, byte minValue) {
		this();
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.maxValue = maxValue;
		this.minValue = minValue;
	}

	@Override
	public Long createScriptValue(Byte value) {
		return transformToMajorType(value, null);
	}
	
	@Override
	public Byte transformFromMajorType(IntegerType majorType, Object value) {
		long v = (Long) value;
		if(v>Byte.MAX_VALUE || v<Byte.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (byte) v;
	}
	
	@Override
	public Long transformToMajorType(Byte value, IntegerType majorType) {
		return (long)value;
	}
}
