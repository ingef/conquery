package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.NumberParsing;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DECIMAL")
public class DecimalType extends CType<BigDecimal, DecimalType> {

	private int maxScale = Integer.MIN_VALUE;
	private BigDecimal maxAbs;
	
	public DecimalType() {
		super(MajorTypeId.DECIMAL, BigDecimal.class);
	}
	
	@Override
	protected void registerValue(BigDecimal v) {
		BigDecimal abs = v.abs();
		if(v.scale() > maxScale)
			maxScale = v.scale();
		if(maxAbs == null || maxAbs.compareTo(abs)<0) {
			maxAbs = abs;
		}
	}
	
	@Override
	public CType<?, DecimalType> bestSubType() {
		if (getLines() > 0 && DecimalTypeScaled.unscale(maxScale, maxAbs).bitLength() <= 63) {
			IntegerType sub = new IntegerType();
			sub.registerValue(DecimalTypeScaled.unscale(maxScale, maxAbs).longValueExact());
			sub.registerValue(-DecimalTypeScaled.unscale(maxScale, maxAbs).longValueExact());
			return new DecimalTypeScaled(getLines(), getNullLines(), maxScale, sub.bestSubType());
		}
		return this;
	}
	
	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseBig(value);
	}
}