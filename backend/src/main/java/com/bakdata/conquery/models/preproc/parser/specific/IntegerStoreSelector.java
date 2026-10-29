package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.events.stores.primitive.ByteArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.IntArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.LongArrayStore;
import com.bakdata.conquery.models.events.stores.primitive.ShortArrayStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.specific.RebasingIntegerStore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
class IntegerStoreSelector {

	private long minValue = Long.MAX_VALUE;
	private long maxValue = Long.MIN_VALUE;

	public void register(long value) {
		if (value > maxValue) {
			maxValue = value;
		}
		if (value < minValue) {
			minValue = value;
		}
	}

	public boolean isEmpty() {
		return minValue > maxValue;
	}

	public IntegerStore decideType(int storeLines) {
		if (isEmpty()) {
			throw new IllegalStateException(String.format("Min (%d) > Max(%d)", minValue, maxValue));
		}

		final long span;

		try {
			// avoids overflows.
			span = Math.subtractExact(maxValue, minValue);
		}
		catch (ArithmeticException exception) {
			return LongArrayStore.create(storeLines);
		}

		// Create minimally required store.
		// if values are in value range without rebasing, skip rebasing as that's faster.
		// max value is reserved for NULL

		if (span + 1 <= (long) Byte.MAX_VALUE - (long) Byte.MIN_VALUE) {
			if (minValue >= Byte.MIN_VALUE && maxValue + 1 <= Byte.MAX_VALUE) {
				return ByteArrayStore.create(storeLines);
			}

			return new RebasingIntegerStore(minValue, Byte.MIN_VALUE, ByteArrayStore.create(storeLines));
		}

		if (span + 1 <= (long) Short.MAX_VALUE - (long) Short.MIN_VALUE) {
			if (minValue >= Short.MIN_VALUE && maxValue + 1 <= Short.MAX_VALUE) {
				return ShortArrayStore.create(storeLines);
			}

			return new RebasingIntegerStore(minValue, Short.MIN_VALUE, ShortArrayStore.create(storeLines));
		}

		if (span + 1 <= (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE) {
			if (minValue >= Integer.MIN_VALUE && maxValue + 1 <= Integer.MAX_VALUE) {
				return IntArrayStore.create(storeLines);
			}

			return new RebasingIntegerStore(minValue, Integer.MIN_VALUE, IntArrayStore.create(storeLines));
		}

		if (maxValue == Long.MAX_VALUE) {
			throw new IllegalStateException("Exceeds capacity of LongStore."); // todo migrate to external null-store
		}

		return LongArrayStore.create(storeLines);
	}
}
