package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="MONEY_INT16")
@Getter @Setter
public class MoneyTypeShort extends MinorCType<Short, MoneyType> {

	private short maxValue;
	private short minValue;
	
	public MoneyTypeShort() {
		super(MajorTypeId.MONEY, short.class);
	}
	
	@JsonCreator
	public MoneyTypeShort(long lines, long nullLines, short maxValue, short minValue) {
		this();
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.maxValue = maxValue;
		this.minValue = minValue;
	}

	@Override
	public Long createScriptValue(Short value) {
		return transformToMajorType(value, null);
	}
	
	@Override
	public Short transformFromMajorType(MoneyType majorType, Object value) {
		long v = (Long) value;
		if(v>Short.MAX_VALUE || v<Short.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (short) v;
	}
	
	@Override
	public Long transformToMajorType(Short value, MoneyType majorType) {
		return (long)value;
	}
}
