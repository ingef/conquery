package com.bakdata.conquery.models.types.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.ColumnStore;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.MoneyType;
import com.bakdata.conquery.util.NumberParsing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<Long> {

	@JsonIgnore
	@Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10).pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());
	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	public MoneyParser(ParserConfig config) {

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
	protected ColumnStore<Long> decideType() {
		IntegerParser subParser = new IntegerParser();
		subParser.registerValue(maxValue);
		subParser.registerValue(minValue);
		subParser.setLines(getLines());
		subParser.setNullLines(getNullLines());
		ColumnStore<Long> subDecision = subParser.findBestType();

		return new MoneyType(subDecision);
	}

}
