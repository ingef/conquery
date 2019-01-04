package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DECIMAL")
public class DecimalType extends CType<BigDecimal, DecimalType> {

	private int maxScale = -1; 
	private BigInteger maxUnscaled;
	private BigInteger minUnscaled;
	
	public DecimalType() {
		super(MajorTypeId.DECIMAL, BigDecimal.class);
	}
	
	@Override
	protected void registerValue(BigDecimal v) {
		BigInteger unscaled = v.unscaledValue();
		if(maxUnscaled == null || maxUnscaled.compareTo(unscaled)<0) {
			maxUnscaled = unscaled;
		}
		if(minUnscaled == null || minUnscaled.compareTo(unscaled)>0) {
			minUnscaled = unscaled;
		}
		if(v.scale() > maxScale)
			maxScale = v.scale();
	}
	
	@Override
	public CType<?, DecimalType> bestSubType() {
		if (getLines() > 0 && maxUnscaled.bitLength() <= 63 && minUnscaled.bitLength() <= 63) {
			IntegerType sub = new IntegerType();
			sub.registerValue(maxUnscaled.longValueExact());
			sub.registerValue(minUnscaled.longValueExact());
			return new DecimalTypeScaled(getLines(), getNullLines(), maxScale, sub);
		}
        else
            return this;
	}
	
	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseBig(value);
	}
}