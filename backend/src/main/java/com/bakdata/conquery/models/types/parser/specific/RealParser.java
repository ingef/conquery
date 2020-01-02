package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.RealTypeDouble;
import com.bakdata.conquery.util.NumberParsing;

public class RealParser extends Parser<Double> {

	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	@Override
	protected Decision<Double, ?, ? extends CType<Double, ?>> decideType() {
		return new Decision<>(new NoopTransformer<Double>(), new RealTypeDouble());
	}
}
