package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.events.stores.RebasingStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.integer.IntegerType;
import com.bakdata.conquery.util.NumberParsing;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
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
	protected IntegerType decideType() {
		long span = maxValue - minValue;

		// max value is reserved for NULL
		if (span + 1 < Byte.MAX_VALUE - Byte.MIN_VALUE) {
			return new IntegerType(new RebasingStore(minValue - (long) Byte.MIN_VALUE, ByteStore.create(getLines())));
		}

		if (span + 1 < Short.MAX_VALUE - Short.MIN_VALUE) {
			return new IntegerType(new RebasingStore(minValue - (long) Short.MIN_VALUE, ShortStore.create(getLines())));
		}

		if (span + 1 < (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
			return new IntegerType(new RebasingStore(minValue - (long) Integer.MIN_VALUE, IntegerStore.create(getLines())));
		}

		return new IntegerType(LongStore.create(getLines()));
	}

}
