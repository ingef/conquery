package com.bakdata.conquery.models.types.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.MoneyTypeLong;
import com.bakdata.conquery.models.types.specific.MoneyTypeVarInt;
import com.bakdata.conquery.util.NumberParsing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class MoneyParser extends Parser<Long> {

	@JsonIgnore
	@Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10)
													 .pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());
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
	protected CType<Long> decideType() {
		if (maxValue + 1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			IntegerParser subParser = new IntegerParser();
			subParser.registerValue(maxValue);
			subParser.registerValue(minValue);
			subParser.setLines(getLines());
			subParser.setNullLines(getNullLines());
			CType<Long> subDecision = subParser.findBestType();
			return new MoneyTypeVarInt(subDecision);
		}
		return new MoneyTypeLong(LongStore.create(getLines()));
	}

}
