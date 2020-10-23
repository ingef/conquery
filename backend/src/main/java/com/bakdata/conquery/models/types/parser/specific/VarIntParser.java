package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.VarIntType;
import com.bakdata.conquery.models.types.specific.VarIntTypeByte;
import com.bakdata.conquery.models.types.specific.VarIntTypeInt;
import com.bakdata.conquery.models.types.specific.VarIntTypeShort;
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
	public Decision<VarIntType> findBestType() {
		return (Decision<VarIntType>) super.findBestType();
	}
	
	@Override
	public Decision<VarIntType> decideType() {
		if(maxValue+1 <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new Decision<>(
					new VarIntTypeByte((byte) minValue, (byte) maxValue, ByteStore.create(getLines()))
			);
		}
		if(maxValue+1 <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new Decision<VarIntType>(

				new VarIntTypeShort((short)minValue, (short)maxValue, ShortStore.create(getLines()))
			);
		}
		return new Decision<VarIntType>(
				new VarIntTypeInt(minValue, maxValue, IntegerStore.create(getLines()))
		);
	}
}
