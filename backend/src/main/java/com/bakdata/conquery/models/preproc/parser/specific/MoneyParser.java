package com.bakdata.conquery.models.preproc.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import com.bakdata.conquery.util.NumberParsing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<Long, MoneyStore> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	@JsonIgnore
	private final BigDecimal moneyFactor;

	public MoneyParser(ConqueryConfig config) {
		super(config);
		moneyFactor = BigDecimal.valueOf(10).pow(config.getPreprocessor().getParsers().getCurrency().getDefaultFractionDigits());
	}

	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing
					   .parseMoney(value)
					   .multiply(moneyFactor)
					   .longValueExact();
	}

	@Override
	protected void registerValue(Long v) {
		if (v > maxValue) {
			maxValue = v;
		}
		if (v < minValue) {
			minValue = v;
		}
	}

	@Override
	protected MoneyStore decideType() {
		IntegerParser subParser = new IntegerParser(getConfig());
		subParser.registerValue(maxValue);
		subParser.registerValue(minValue);
		subParser.setLines(getLines());
		subParser.setNullLines(getNullLines());
		IntegerStore subDecision = subParser.findBestType();

		return new MoneyIntStore(subDecision);
	}

	@Override
	public void setValue(MoneyStore store, int event, Long value) {
		store.setMoney(event, value);
	}

	@Override
	public ColumnValues createColumnValues() {
		return new LongColumnValues();
	}

}
