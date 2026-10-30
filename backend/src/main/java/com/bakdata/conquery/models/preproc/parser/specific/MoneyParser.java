package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;
import java.util.Objects;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<BigDecimal, MoneyStore, BigDecimalColumnValues> {

	private final int defaultFractionDigits;
	private BigDecimal maxValue = null;
	private BigDecimal minValue = null;

	public MoneyParser(ConqueryConfig config) {
		super(config, new BigDecimalColumnValues());
		defaultFractionDigits = config.getPreprocessor().getParsers().getCurrency().getDefaultFractionDigits();
	}

	public void addMoney(BigDecimal value) {
		Objects.requireNonNull(value, "value");
		recordLine();
		if (maxValue == null){
			maxValue = value;
		}
		if(minValue == null){
			minValue = value;
		}

		maxValue = maxValue.max(value);
		minValue = minValue.min(value);
		getColumnValues().addBigDecimal(value);
	}

	protected MoneyStore decideType() {
		return decideType(getLines());
	}

	@Override
	protected MoneyStore decideType(int storeLines) {
		IntegerStoreSelector selector = new IntegerStoreSelector();
		selector.setMaxValue(maxValue.movePointRight(defaultFractionDigits).longValue());
		selector.setMinValue(minValue.movePointRight(defaultFractionDigits).longValue());
		IntegerStore subDecision = selector.decideType(storeLines);

		return new MoneyIntStore(subDecision, defaultFractionDigits);
	}

	@Override
	public void setValue(MoneyStore store, int event, BigDecimal value) {
		store.setMoney(event, value);
	}

}
