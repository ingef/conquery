package com.bakdata.conquery.models.events.stores.primitive;

import java.util.Arrays;
import java.util.BitSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.BitSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.BitSetSerializer;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Stores boolean values as bits. Can therefore not store null.
 */
@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class BitSetStore implements BooleanStore {

	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet values;

	@JsonCreator
	public BitSetStore(BitSet values) {
		this.values = values;
	}

	public static BitSetStore create(int size) {
		return new BitSetStore(new BitSet(size));
	}

	@Override
	public int getLines() {
		return values.length();
	}

	@Override
	public long estimateEventBits() {
		return 1;
	}

	@Override
	public void set(int event, @Nullable Object value) {
		if (value == null) {
			set(event, false);
			return;
		}

		values.set(event, (Boolean) value);
	}

	public BitSetStore select(int[] starts, int[] lengths) {
		int length = Arrays.stream(lengths).sum();

		final BitSet out = new BitSet(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			for (int bit = 0; bit < lengths[index]; bit++) {
				out.set(pos + bit, getValues().get(starts[index] + bit));
			}
			pos += lengths[index];
		}

		return new BitSetStore(out);
	}


	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public Boolean get(int event) {
		return getBoolean(event);
	}

	@Override
	public boolean getBoolean(int event) {
		return values.get(event);
	}
}
