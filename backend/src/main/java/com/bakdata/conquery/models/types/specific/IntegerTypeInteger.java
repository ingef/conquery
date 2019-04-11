package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="INTEGER_INT32")
@Getter @Setter
public class IntegerTypeInteger extends MinorCType<Integer, IntegerType> {

	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	public IntegerTypeInteger() {
		super(MajorTypeId.INTEGER, int.class);
	}
	
	@JsonCreator
	public IntegerTypeInteger(long lines, long nullLines, int maxValue, int minValue) {
		this();
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.maxValue = maxValue;
		this.minValue = minValue;
	}


	@Override
	public Long createScriptValue(Integer value) {
		return transformToMajorType(value, null);
	}
	
	@Override
	protected void registerValue(Integer v) {
		if(v > maxValue) {
			maxValue = v;
		}
		if(v < minValue) {
			minValue = v;
		}
	}
	
	@Override
	public Integer transformFromMajorType(IntegerType majorType, Object value) {
		long v = (Long) value;
		if(v>Integer.MAX_VALUE || v<Integer.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (int) v;
	}
	
	@Override
	public Long transformToMajorType(Integer value, IntegerType majorType) {
		return (long)value;
	}
}
