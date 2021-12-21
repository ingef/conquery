package com.bakdata.conquery.models.events.stores.primitive;

import java.util.Arrays;
import java.util.BitSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.BitSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.BitSetSerializer;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Stores boolean values as bits and an auxiliary null-bit.
 */
@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class BitSetStore implements BooleanStore {

	//TODO now that this class is in use, consider using RoaringBitmaps
	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet values;

	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet nullBits;

	@ToString.Include
	@Getter(onMethod_ = @JsonIgnore(value = false))
	private final int lines;

	public static BitSetStore create(int size) {
		return new BitSetStore(new BitSet(size), new BitSet(size), size);
	}


	@Override
	public long estimateEventBits() {
		return 2;
	}

	@Override
	public void setBoolean(int event, boolean value) {
		nullBits.set(event, true);
		values.set(event, value);
	}

	@Override
	public BitSetStore select(int[] starts, int[] lengths) {
		int length = Arrays.stream(lengths).sum();

		final BitSet out = new BitSet(length);
		final BitSet outNulls = new BitSet(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			for (int bit = 0; bit < lengths[index]; bit++) {

				out.set(pos + bit, getBoolean(starts[index] + bit));
				outNulls.set(pos + bit, !has(starts[index] + bit));
			}
			pos += lengths[index];
		}

		return new BitSetStore(out, outNulls, length);
	}


	@Override
	public boolean has(int event) {
		return !safeGetFromStore(event, nullBits, getLines());
	}

	@Override
	public void setNull(int event) {
		nullBits.set(event);
	}

	@Override
	public boolean getBoolean(int event) {
		return safeGetFromStore(event, values, getLines());
	}

	private static boolean safeGetFromStore(int event, BitSet bits, int lines) {
		if (event >= lines) {
			throw new IllegalArgumentException(String.format("Trying to access line %d beyond %d lines", event, lines));
		}

		// Dangling false causes issues
		if (event + 1 > bits.length()) {
			return false;
		}
		return bits.get(event);
	}

	@ValidationMethod(message = "Values are longer than actual lines.")
	public boolean lengthsMatchValues() {
		return values.length() < getLines();
	}

	@ValidationMethod(message = "NullBits are longer than actual lines.")
	public boolean lengthsMatchNulls() {
		return nullBits.length() < getLines();
	}
}
