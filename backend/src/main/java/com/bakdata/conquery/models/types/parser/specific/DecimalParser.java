package com.bakdata.conquery.models.types.parser.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.DecimalTypeBigDecimal;
import com.bakdata.conquery.models.types.specific.DecimalTypeScaled;
import com.bakdata.conquery.util.NumberParsing;
import lombok.NonNull;

public class DecimalParser extends Parser<BigDecimal> {

	private transient int maxScale = Integer.MIN_VALUE;
	private transient BigDecimal maxAbs;
	
	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseBig(value);
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
	protected Decision<BigDecimal, ?, ? extends CType<BigDecimal, ?>> decideType() {
		if(getLines() > 0) {
			BigInteger unscaled = DecimalTypeScaled.unscale(maxScale, maxAbs);
			if(unscaled.bitLength() <= 63) {
				IntegerParser sub = new IntegerParser();
				sub.registerValue(unscaled.longValueExact());
				sub.registerValue(-unscaled.longValueExact());
				sub.setLines(getLines());
				sub.setNullLines(getNullLines());
				Decision<Long, Number, ? extends CType<Long, ? extends Number>> subDecision = sub.findBestType();
				return new Decision<BigDecimal, Number, DecimalTypeScaled>(
					new Transformer<BigDecimal, Number>() {
						@Override
						public Number transform(@NonNull BigDecimal value) {
							return subDecision.getTransformer().transform(
								DecimalTypeScaled.unscale(maxScale,value).longValueExact()
							);
						}
					},
					new DecimalTypeScaled(maxScale, subDecision.getType())
				);
			}
		}
		return new Decision<BigDecimal, BigDecimal, DecimalTypeBigDecimal>(
			new NoopTransformer<>(),
			new DecimalTypeBigDecimal()
		);
	}

}
