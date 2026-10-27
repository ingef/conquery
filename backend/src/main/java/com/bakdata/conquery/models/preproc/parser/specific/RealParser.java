package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double, RealStore> {

	private final double requiredPrecision;

	private double floatULP = Float.NEGATIVE_INFINITY;

	public RealParser(ConqueryConfig config) {
		super(config, new DoubleColumnValues());
		requiredPrecision = config.getPreprocessor().getParsers().getMinPrecision();
	}

	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	@Override
	public int parseAndAdd(String value) throws ParsingException {
		if (value == null) {
			return add(null);
		}

		try {
			return addDouble(NumberParsing.parseDouble(value));
		}
		catch (Exception e) {
			throw parsingException(value, e);
		}
	}

	/**
	 * Collect ULP of all values
	 *
	 * @see Math#ulp(float) for an explanation.
	 */
	@Override
	protected void registerValue(Double v) {
		registerValue(v.doubleValue());
	}

	private void registerValue(double value) {
		floatULP = Math.max(floatULP, Math.ulp((float) value));
	}

	public int addDouble(double value) {
		recordDoubleLine(value);
		registerValue(value);
		return ((DoubleColumnValues) requireColumnValues()).addDouble(value);
	}

	/**
	 * If values are within a margin of precision, we store them as floats.
	 */
	@Override
	protected RealStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected RealStore decideType(int storeLines) {
		log.debug("Max ULP = {}", floatULP);

		if (floatULP < requiredPrecision) {
			return FloatArrayStore.create(storeLines);
		}
		else {
			return DoubleArrayStore.create(storeLines);
		}
	}

	@Override
	public void setValue(RealStore store, int event, Double value) {
		store.setReal(event, value);
	}

}
