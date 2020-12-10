package com.bakdata.conquery.models.events.parser.specific;

import com.bakdata.conquery.models.events.parser.Parser;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.ByteStore;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.events.stores.base.LongStore;
import com.bakdata.conquery.models.events.stores.base.ShortStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.util.NumberParsing;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IntegerParser extends Parser<Long> {

	private long minValue = Long.MAX_VALUE;
	private long maxValue = Long.MIN_VALUE;


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
		long span = maxValue - minValue;

		//TODO if values are in value range without rebasing, skip rebasing

		// max value is reserved for NULL
		if (span + 1 < (long) Byte.MAX_VALUE - (long)  Byte.MIN_VALUE) {
			return new RebasingStore(minValue, (long) Byte.MIN_VALUE, ByteStore.create(getLines()));
		}

		if (span + 1 < (long) Short.MAX_VALUE - (long)  Short.MIN_VALUE) {
			return new RebasingStore(minValue, Short.MIN_VALUE, ShortStore.create(getLines()));
		}

		if (span + 1 < (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
			return new RebasingStore(minValue, Integer.MIN_VALUE, IntegerStore.create(getLines()));
		}

		return new RebasingStore(minValue, Long.MIN_VALUE, LongStore.create(getLines()));
	}

}
