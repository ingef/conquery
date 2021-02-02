package com.bakdata.conquery.models.events.parser.specific;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.DecimalStore;
import com.bakdata.conquery.models.events.stores.specific.DecimalTypeScaled;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString(callSuper = true)
@Slf4j
public class DecimalParser extends Parser<BigDecimal> {

	private transient int maxScale = Integer.MIN_VALUE;
	private transient BigDecimal maxAbs;

	public DecimalParser(ParserConfig config) {
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
	protected ColumnStore<BigDecimal> decideType() {

		BigInteger unscaled = DecimalTypeScaled.unscale(maxScale, maxAbs);
		if (unscaled.bitLength() > 63) {
			return DecimalStore.create(getLines());
		}

		IntegerParser sub = new IntegerParser(getConfig());
		sub.setMaxValue(unscaled.longValueExact());
		sub.setMinValue(-unscaled.longValueExact());
		sub.setLines(getLines());
		sub.setNullLines(getNullLines());
		ColumnStore<Long> subDecision = sub.findBestType();

		return new DecimalTypeScaled(maxScale, subDecision);
	}

}
