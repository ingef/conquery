package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="DATE") @Getter @Setter
public class DateType extends CType<Integer, DateType> {
	
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	public DateType() {
		super(MajorTypeId.DATE, int.class);
	}
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		//see #148  Delegate to DateUtils instead
		return CDate.ofLocalDate(DateFormats.instance().parseToLocalDate(value));
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
	public CType<? extends Number, DateType> bestSubType() {
		if(maxValue+1 <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new DateTypeByte(getLines(), getNullLines(), (byte)maxValue, (byte)minValue);
		}
		if(maxValue+1 <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new DateTypeShort(getLines(), getNullLines(), (short)maxValue, (short)minValue);
		}
		else {
			return this;
		}
	}

	@Override
	public Object createScriptValue(Integer value) {
		return CDate.toLocalDate(value);
	}
	
	

	@Override
	public Object createPrintValue(Integer value) {
		if (value == null) {
			return "";
		}

		return CDate.toLocalDate(value);
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
}