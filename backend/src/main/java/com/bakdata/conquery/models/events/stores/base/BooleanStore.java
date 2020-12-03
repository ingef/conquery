package com.bakdata.conquery.models.events.stores.base;

import java.util.Arrays;
import java.util.BitSet;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.BitSetDeserializer;
import com.bakdata.conquery.io.jackson.serializer.BitSetSerializer;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.ToString;


@CPSType(id = "BOOLEANS", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class BooleanStore extends ColumnStore<Boolean> {

	@JsonSerialize(using = BitSetSerializer.class)
	@JsonDeserialize(using = BitSetDeserializer.class)
	private final BitSet values;

	@JsonCreator
	public BooleanStore(BitSet values) {
		this.values = values;
	}

	public static BooleanStore create(int size) {
		return new BooleanStore(new BitSet(size));
	}

	@Override
	public long estimateEventBytes() {
		return 1; // over estimates factor 4
	}

	public BooleanStore select(int[] starts, int[] lengths) {
		int length = Arrays.stream(lengths).sum();

		final BitSet out = new BitSet(length);

		int pos = 0;

		for (int index = 0; index < starts.length; index++) {
			for (int bit = 0; bit < lengths[index]; bit++) {
				out.set(pos + bit, getValues().get(starts[index] + bit));
			}
			pos += lengths[index];
		}

		return new BooleanStore(out);
	}

	@Override
	public void set(int event, Boolean value) {
		if (value == null) {
			set(event, false);
			return;
		}

		values.set(event, value);
	}

	@Override
	public boolean has(int event) {
		return true;
	}

	@Override
	public Boolean get(int event) {
		return values.get(event);
	}
}
