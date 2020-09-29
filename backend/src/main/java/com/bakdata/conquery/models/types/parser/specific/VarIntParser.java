package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.VarIntTypeByte;
import com.bakdata.conquery.models.types.specific.VarIntTypeInt;
import com.bakdata.conquery.models.types.specific.VarIntTypeShort;
import lombok.NonNull;
import lombok.ToString;

@ToString(callSuper = true)
public class VarIntParser extends Parser<Integer> {

	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	
	@Override
	public void registerValue(Integer v) {
		if(v > maxValue) {
			maxValue = v;
		}
		if(v < minValue) {
			minValue = v;
		}
	}
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Decision<Integer, Number, VarIntType> findBestType() {
		return (Decision<Integer, Number, VarIntType>) super.findBestType();
	}
	
	@Override
	public Decision<Integer, Number, VarIntType> decideType() {
		if(maxValue+1 <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new Decision<Integer, Number, VarIntType>(
				new Transformer<Integer, Number>() {
					@Override
					public Number transform(@NonNull Integer value) {
						return value.byteValue();
					}
				},
				new VarIntTypeByte((byte)minValue, (byte)maxValue)
			);
		}
		if(maxValue+1 <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new Decision<Integer, Number, VarIntType>(
				new Transformer<Integer, Number>() {
					@Override
					public Number transform(@NonNull Integer value) {
						return value.shortValue();
					}
				},
				new VarIntTypeShort((short)minValue, (short)maxValue)
			);
		}
		else {
			return new Decision<Integer, Number, VarIntType>(
				new Transformer<Integer, Number>() {
					@Override
					public Number transform(@NonNull Integer value) {
						return value;
					}
				},
				new VarIntTypeInt(minValue, maxValue)
			);
		}
	}
}
