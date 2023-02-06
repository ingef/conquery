package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DecimalArrayStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.specific.ScaledDecimalStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString(callSuper = true)
@Slf4j
public class DecimalParser extends Parser<BigDecimal, DecimalStore> {

	private transient int maxScale = Integer.MIN_VALUE;
	private transient BigDecimal maxAbs;

	public DecimalParser(ConqueryConfig config) {
		super(config);
	}

	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseBig(value);
	}

	@Override
	protected void registerValue(BigDecimal v) {
		log.trace("Registering `{}`", v);

		BigDecimal abs = v.abs();
		if (v.scale() > maxScale) {
			maxScale = v.scale();
		}
		if (maxAbs == null || maxAbs.compareTo(abs) < 0) {
			maxAbs = abs;
		}
	}

	@Override
	protected DecimalStore decideType() {

		BigInteger unscaled = ScaledDecimalStore.unscale(maxScale, maxAbs);
		if (unscaled.bitLength() > 63) {
			return DecimalArrayStore.create(getLines());
		}

		IntegerParser sub = new IntegerParser(getConfig());
		sub.setMaxValue(unscaled.longValueExact());
		sub.setMinValue(-unscaled.longValueExact());
		sub.setLines(getLines());
		sub.setNullLines(getNullLines());
		IntegerStore subDecision = sub.findBestType();

		return new ScaledDecimalStore(maxScale, subDecision);
	}

	@Override
	public void setValue(DecimalStore store, int event, BigDecimal value) {
		store.setDecimal(event, value);
	}

	@Override
	public ColumnValues<BigDecimal> createColumnValues() {
		return new ListColumnValues();
	}

}
