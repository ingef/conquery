package com.bakdata.conquery.models.events.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.specific.MoneyIntStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.NumberParsing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<Long, MoneyStore> {

	@JsonIgnore
	@Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10).pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());
	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	public MoneyParser(ParserConfig config) {
		super(config);
	}

	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing
					   .parseMoney(value)
					   .multiply(getMoneyFactor())
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
		return new ColumnValues(new LongArrayList(), 0);
	}

}
