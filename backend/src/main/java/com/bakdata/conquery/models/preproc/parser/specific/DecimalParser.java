package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.primitive.DecimalArrayStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.specific.ScaledDecimalStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;

@ToString(callSuper = true)
public class DecimalParser extends Parser<BigDecimal, DecimalStore, BigDecimalColumnValues> {

	private transient int maxScale = Integer.MIN_VALUE;
	private transient BigDecimal maxAbs;

	public DecimalParser(ConqueryConfig config) {
		super(config, new BigDecimalColumnValues());
	}

	public void addDecimal(BigDecimal value) {
		Objects.requireNonNull(value, "value");
		recordLine();
		final BigDecimal abs = value.abs();
		if (value.scale() > maxScale) {
			maxScale = value.scale();
		}
		if (maxAbs == null || maxAbs.compareTo(abs) < 0) {
			maxAbs = abs;
		}
		getColumnValues().addBigDecimal(value);
	}

	protected DecimalStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected DecimalStore decideType(int storeLines) {

		BigInteger unscaled = ScaledDecimalStore.unscale(maxScale, maxAbs);
		if (unscaled.bitLength() > 63) {
			return DecimalArrayStore.create(storeLines);
		}

		IntegerStoreSelector selector = new IntegerStoreSelector();
		selector.setMaxValue(unscaled.longValueExact());
		selector.setMinValue(-unscaled.longValueExact());
		IntegerStore subDecision = selector.decideType(storeLines);

		return new ScaledDecimalStore(maxScale, subDecision);
	}

	@Override
	public void setValue(DecimalStore store, int event, BigDecimal value) {
		store.setDecimal(event, value);
	}

}
