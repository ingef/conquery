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
import lombok.Getter;
import lombok.ToString;

/**
 * Stores boolean values as bits and an auxiliary null-bit.
 */
@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class BitSetStore implements BooleanStore {
	//TODO now that this class is in use, consider using RoaringBitmaps
	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet values;

	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet nullBits;

	@JsonIgnore
	private final int lines;

	@JsonCreator
	public BitSetStore(BitSet values, BitSet nullBits) {
		this.values = values;
		this.nullBits = nullBits;

		// dangling false is not counted
		lines = Math.max(values.length(), nullBits.length());
	}

	public static BitSetStore create(int size) {
		return new BitSetStore(new BitSet(size), new BitSet(size));
	}


	@Override
	public long estimateEventBits() {
		return 2;
	}

	@Override
	public void setBoolean(int event, boolean value) {
		values.set(event, value);
	}

	public BitSetStore select(int[] starts, int[] lengths) {
		int length = Arrays.stream(lengths).sum();

		final BitSet out = new BitSet(length);
		final BitSet outNulls = new BitSet(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			for (int bit = 0; bit < lengths[index]; bit++) {
				out.set(pos + bit, getValues().get(starts[index] + bit));

				outNulls.set(pos + bit, getNullBits().get(starts[index] + bit));
			}
			pos += lengths[index];
		}

		return new BitSetStore(out, outNulls);
	}


	@Override
	public boolean has(int event) {
		return !nullBits.get(event);
	}

	@Override
	public void setNull(int event) {
		nullBits.set(event);
	}

	@Override
	public boolean getBoolean(int event) {
		return values.get(event);
	}
}
