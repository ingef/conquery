package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.integer.IntegerTypeLong;
import com.bakdata.conquery.models.types.specific.integer.VarIntTypeByte;
import com.bakdata.conquery.models.types.specific.integer.VarIntTypeInt;
import com.bakdata.conquery.models.types.specific.integer.VarIntTypeShort;
import com.bakdata.conquery.util.NumberParsing;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter @Setter
public class IntegerParser extends Parser<Long> {

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;

	public IntegerParser() {

	}

	@Override
	protected Long parseValue(String value) throws ParsingException {
		return NumberParsing.parseLong(value);
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
	public CType<Long> findBestType() {
		return super.findBestType();
	}

	@Override
	protected CType<Long> decideType() {
		if (maxValue + 1 <= Byte.MAX_VALUE && minValue >= Byte.MIN_VALUE) {
			return new VarIntTypeByte((byte) minValue, (byte) maxValue, ByteStore.create(getLines()));
		}

		if (maxValue + 1 <= Short.MAX_VALUE && minValue >= Short.MIN_VALUE) {
			return new VarIntTypeShort((short) minValue, (short) maxValue, ShortStore.create(getLines()));
		}

		if (maxValue + 1 <= Integer.MAX_VALUE && minValue >= Integer.MIN_VALUE) {
			return new VarIntTypeInt((int) minValue, (int) maxValue, IntegerStore.create(getLines()));

		}

		return new IntegerTypeLong(minValue, maxValue, LongStore.create(getLines()));
	}

}
