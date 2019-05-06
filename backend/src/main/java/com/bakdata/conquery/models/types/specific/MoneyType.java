package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@CPSType(base=CType.class, id="MONEY")
public class MoneyType extends CType<Long, MoneyType> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;
	@JsonIgnore @Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10)
		.pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());
	
	public MoneyType() {
		super(MajorTypeId.MONEY, long.class);
	}
	
	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing
			.parseMoney(value)
			.multiply(getMoneyFactor())
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
		if(maxValue+1 <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new MoneyTypeByte(getLines(), getNullLines(), (byte)maxValue, (byte)minValue);
		}
		if(maxValue+1 <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new MoneyTypeShort(getLines(), getNullLines(), (short)maxValue, (short)minValue);
		}
		if(maxValue+1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			return new MoneyTypeInteger(getLines(), getNullLines(), (int)maxValue, (int)minValue);
		}
		else {
			return this;
		}
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
}