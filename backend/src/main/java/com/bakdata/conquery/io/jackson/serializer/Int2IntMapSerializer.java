package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import lombok.Data;

public class Int2IntMapSerializer extends StdSerializer<Int2IntArrayMap> {
	@Data
	public static class SerializationContainer {
		private final List<Integer> keys;
		private final List<Integer> values;
		private final int defaultValue;
	}

	protected Int2IntMapSerializer() {
		super(Int2IntArrayMap.class);
	}

	@Override
	public void serialize(Int2IntArrayMap map, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final ArrayList<Integer> keys = new ArrayList<>(map.size());
		final ArrayList<Integer> values = new ArrayList<>(map.size());

		map.forEach((k, v) -> {
			keys.add(k);
			values.add(v);
		});

		gen.writeObject(new SerializationContainer(keys, values, map.defaultReturnValue()));
	}
}
