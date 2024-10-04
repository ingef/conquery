package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<BigDecimal, MoneyStore> {

	private final int defaultFractionDigits;
	private BigDecimal maxValue = null;
	private BigDecimal minValue = null;

	public MoneyParser(ConqueryConfig config) {
		super(config);
		defaultFractionDigits = config.getPreprocessor().getParsers().getCurrency().getDefaultFractionDigits();
	}

	@Override
	protected BigDecimal parseValue(String value) throws ParsingException {
		return NumberParsing.parseMoney(value);
	}

	@Override
	protected void registerValue(BigDecimal v) {
		if (maxValue == null){
			maxValue = v;
		}
		if(minValue == null){
			minValue = v;
		}

		maxValue = maxValue.max(v);
		minValue = minValue.min(v);
	}

	@Override
	protected MoneyStore decideType() {
		IntegerParser subParser = new IntegerParser(getConfig());
		subParser.registerValue(maxValue.movePointRight(defaultFractionDigits).longValue());
		subParser.registerValue(minValue.movePointRight(defaultFractionDigits).longValue());
		subParser.setLines(getLines());
		subParser.setNullLines(getNullLines());
		IntegerStore subDecision = subParser.findBestType();

		return new MoneyIntStore(subDecision, defaultFractionDigits);
	}

	@Override
	public void setValue(MoneyStore store, int event, BigDecimal value) {
		store.setMoney(event, value);
	}

	@Override
	public ColumnValues createColumnValues() {
		return new ListColumnValues();
	}

}
