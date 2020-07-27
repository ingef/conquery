package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.RealTypeDouble;
import com.bakdata.conquery.models.types.specific.RealTypeFloat;
import com.bakdata.conquery.util.NumberParsing;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double> {

	private double floatULP = Float.NEGATIVE_INFINITY;

	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	@Override
	protected void registerValue(Double v) {
		floatULP = Math.max(floatULP, Math.ulp(v.floatValue()));
	}

	@Override
	protected Decision<Double, ?, ? extends CType<Double, ?>> decideType() {
		// TODO: 27.07.2020 FK: Make this configurable
		log.debug("Max Float ULP = {}", floatULP);

		if(floatULP < 1e-4){
			return new Decision<>(
					new Transformer<Double, Float>() {
						@Override
						public Float transform(@NonNull Double value) {
							return value.floatValue();
						}
					},
					new RealTypeFloat()
			);
		}


		return new Decision<>(new NoopTransformer<Double>(), new RealTypeDouble());
	}
}
