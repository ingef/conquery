package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.NoopTransformer;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.IntegerTypeLong;
import com.bakdata.conquery.models.types.specific.IntegerTypeVarInt;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.util.NumberParsing;
import lombok.NonNull;
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
	public Decision<Long, Number, ? extends CType<Long, ? extends Number>> findBestType() {
		return (Decision<Long, Number, ? extends CType<Long, ? extends Number>>) super.findBestType();
	}

	@Override
	protected Decision<Long, ?, ? extends CType<Long, ?>> decideType() {
		if(maxValue+1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			VarIntParser subParser = new VarIntParser();
			subParser.registerValue((int)maxValue);
			subParser.registerValue((int)minValue);
			subParser.setLines(getLines());
			subParser.setNullLines(getNullLines());
			Decision<Integer, Number, VarIntType> subDecision = subParser.findBestType();
			return new Decision<>(
				new Transformer<Long, Number>() {
					@Override
					public Number transform(@NonNull Long value) {
						return subDecision.getTransformer().transform(value.intValue());
					}
				},
				new IntegerTypeVarInt(subDecision.getType())
			);
		}
		return new Decision<Long, Long, IntegerTypeLong>(
			new NoopTransformer<>(),
			new IntegerTypeLong(minValue, maxValue)
		);
	}

}
