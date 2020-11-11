package com.bakdata.conquery.models.types.parser.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.MoneyTypeLong;
import com.bakdata.conquery.models.types.specific.MoneyTypeVarInt;
import com.bakdata.conquery.models.types.specific.VarIntType;
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
	protected Decision<? extends CType<Long, ?>> decideType() {
		if (maxValue + 1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			VarIntParser subParser = new VarIntParser();
			subParser.registerValue((int) maxValue);
			subParser.registerValue((int) minValue);
			subParser.setLines(getLines());
			subParser.setNullLines(getNullLines());
			Decision<VarIntType> subDecision = subParser.findBestType();
			return new Decision<>(
					new MoneyTypeVarInt(subDecision.getType())
			);
		}
		return new Decision<MoneyTypeLong>(
				new MoneyTypeLong(LongStore.create(getLines()))
		);
	}

}
