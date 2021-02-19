package com.bakdata.conquery.models.events.parser.specific;

import java.nio.DoubleBuffer;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.ColumnValues;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.primitive.DoubleArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.FloatArrayStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double, RealStore> {

	private final double requiredPrecision;

	private double floatULP = Float.NEGATIVE_INFINITY;

	public RealParser(ParserConfig config) {
		super(config);
		requiredPrecision = config.getMinPrecision();
	}

	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	/**
	 * Collect ULP of all values
	 *
	 * @see Math#ulp(float) for an explanation.
	 */
	@Override
	protected void registerValue(Double v) {
		floatULP = Math.max(floatULP, Math.ulp(v.floatValue()));
	}

	/**
	 * If values are within a margin of precision, we store them as floats.
	 */
	@Override
	protected RealStore decideType() {
		log.debug("Max ULP = {}", floatULP);

		if (floatULP < requiredPrecision) {
			return FloatArrayStore.create(getLines());
		}
		else {
			return DoubleArrayStore.create(getLines());
		}
	}

	@Override
	public void setValue(RealStore store, int event, Double value) {
		store.setReal(event, value);
	}

	@Override
	public ColumnValues createColumnValues() {
		return new ColumnValues<Double>(0d) {

			private final DoubleBuffer buffer = ColumnValues.allocateBuffer().asDoubleBuffer();


			@Override
			public Double get(int event) {
				return buffer.get(event);
			}

			@Override
			protected void write(int event, Double obj) {
				buffer.put(obj);
			}
		};
	}

}
