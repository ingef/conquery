package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double, RealStore, DoubleColumnValues> {

	private final double requiredPrecision;

	private double floatULP = Float.NEGATIVE_INFINITY;

	public RealParser(ConqueryConfig config) {
		super(config, new DoubleColumnValues());
		requiredPrecision = config.getPreprocessor().getParsers().getMinPrecision();
	}

	/**
	 * Collect ULP of all values
	 *
	 * @see Math#ulp(float) for an explanation.
	 */
	public void addDouble(double value) {
		recordLine();
		floatULP = Math.max(floatULP, Math.ulp((float) value));
		getColumnValues().addDouble(value);
	}

	/**
	 * If values are within a margin of precision, we store them as floats.
	 */
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
