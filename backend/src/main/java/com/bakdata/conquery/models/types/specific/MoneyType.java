package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="MONEY")
public class MoneyType extends CType<Long, MoneyType> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;
	
	public MoneyType() {
		super(MajorTypeId.MONEY, long.class);
	}
	
	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing
			.parseMoney(value)
			.multiply(BigDecimal.valueOf(10).pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits()))
			.longValueExact();
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
	public CType<? extends Number, MoneyType> bestSubType() {
		if(maxValue <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new MoneyTypeByte(getLines(), getNullLines(), (byte)maxValue, (byte)minValue);
		}
		if(maxValue <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new MoneyTypeShort(getLines(), getNullLines(), (short)maxValue, (short)minValue);
		}
		if(maxValue <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			return new MoneyTypeInteger(getLines(), getNullLines(), (int)maxValue, (int)minValue);
		}
		else {
			return this;
		}
	}
}