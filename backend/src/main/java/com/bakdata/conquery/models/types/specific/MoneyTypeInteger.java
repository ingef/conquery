package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="MONEY_INT32")
@Getter @Setter
public class MoneyTypeInteger extends MinorCType<Integer, MoneyType> {

	private int maxValue;
	private int minValue;
	
	public MoneyTypeInteger() {
		super(MajorTypeId.MONEY, int.class);
	}
	
	@JsonCreator
	public MoneyTypeInteger(long lines, long nullLines, int maxValue, int minValue) {
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
	public Integer transformFromMajorType(MoneyType majorType, Object value) {
		long v = (Long) value;
		if(v>Integer.MAX_VALUE || v<Integer.MIN_VALUE) {
			throw new IllegalArgumentException(value+" is out of range");
		}
		return (int) v;
	}
	
	@Override
	public Long transformToMajorType(Integer value, MoneyType majorType) {
		return (long)value;
	}
}
