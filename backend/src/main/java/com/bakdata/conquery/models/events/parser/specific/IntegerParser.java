package com.bakdata.conquery.models.events.parser.specific;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.NumberParsing;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
public class IntegerParser extends Parser<Long> {

	private long minValue = Long.MAX_VALUE;
	private long maxValue = Long.MIN_VALUE;

	public IntegerParser(ParserConfig config){
		super(config);
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
	protected ColumnStore<Long> decideType() {
		if (minValue > maxValue) {
			throw new IllegalStateException(String.format("Min (%d) > Max(%d)", minValue, maxValue));
		}

		final long span;

		try {
			// avoids overflows.
			span = Math.subtractExact(maxValue, minValue);
		}
		catch (ArithmeticException exception) {
			return LongStore.create(getLines());
		}

		// Create minimally required store.
		// if values are in value range without rebasing, skip rebasing as that's faster.
		// max value is reserved for NULL

		if (span + 1 <= (long) Byte.MAX_VALUE - (long)  Byte.MIN_VALUE) {
			if(minValue >= Byte.MIN_VALUE && maxValue + 1 <= Byte.MAX_VALUE) {
				return ByteStore.create(getLines());
			}

			return new RebasingStore(minValue, (long) Byte.MIN_VALUE, ByteStore.create(getLines()));
		}

		if (span + 1 <= (long) Short.MAX_VALUE - (long)  Short.MIN_VALUE) {
			if(minValue >= Short.MIN_VALUE && maxValue + 1 <= Short.MAX_VALUE) {
				return ShortStore.create(getLines());
			}

			return new RebasingStore(minValue, Short.MIN_VALUE, ShortStore.create(getLines()));
		}

		if (span + 1 <= (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
			if(minValue >= Integer.MIN_VALUE && maxValue + 1 <= Integer.MAX_VALUE) {
				return IntegerStore.create(getLines());
			}

			return new RebasingStore(minValue, Integer.MIN_VALUE, IntegerStore.create(getLines()));
		}

		if(maxValue == Long.MAX_VALUE){
			throw new IllegalStateException("Exceeds capacity of LongStore."); // todo migrate to external null-store
		}

		return LongStore.create(getLines());
	}

}
