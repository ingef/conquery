package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="INTEGER") @Getter @Setter
public class IntegerType extends CType<Long, IntegerType> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;
	
	public IntegerType() {
		super(MajorTypeId.INTEGER, long.class);
	}
	
	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing.parseLong(value);
	}
	
	@Override
	protected void registerValue(Long v) {
		if(v > maxValue) {
			maxValue = v;
		}
		if(v < minValue) {
			minValue = v;
		}
	}

	@Override
	public CType<? extends Number, IntegerType> bestSubType() {
		if(maxValue <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new IntegerTypeByte(getLines(), getNullLines(), (byte)maxValue, (byte)minValue);
		}
		if(maxValue <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new IntegerTypeShort(getLines(), getNullLines(), (short)maxValue, (short)minValue);
		}
		if(maxValue <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			return new IntegerTypeInteger(getLines(), getNullLines(), (int)maxValue, (int)minValue);
		}
		else {
			return this;
		}
	}
}