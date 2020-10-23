package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.integer.IntegerTypeLong;
import com.bakdata.conquery.models.types.specific.integer.IntegerTypeVarInt;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;

@ToString(callSuper = true)
public class IntegerParser extends Parser<Long> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	public IntegerParser(ParserConfig config) {

	}

	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing.parseLong(value);
	}
	
	@Override
	protected void registerValue(Long v) {
		if(v > maxValue) {
			maxValue = v;
		}
		if(v < minValue) {
			minValue = v;
		}
	}
	
	@Override
	public Decision<? extends CType<Long, ? extends Number>> findBestType() {
		return (Decision<? extends CType<Long, ? extends Number>>) super.findBestType();
	}

	@Override
	protected Decision<? extends CType<Long, ?>> decideType() {
		if(maxValue+1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			VarIntParser subParser = new VarIntParser();
			subParser.registerValue((int)maxValue);
			subParser.registerValue((int)minValue);
			subParser.setLines(getLines());
			subParser.setNullLines(getNullLines());
			Decision<VarIntType> subDecision = subParser.findBestType();
			return new Decision<>(
					new IntegerTypeVarInt(subDecision.getType())
			);
		}
		return new Decision<IntegerTypeLong>(
			new IntegerTypeLong(minValue, maxValue, LongStore.create(getLines()))
		);
	}

}
