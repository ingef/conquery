package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

public class Int2IntArrayMapDeserializer extends StdDeserializer<Int2IntArrayMap> {

	protected Int2IntArrayMapDeserializer() {
		super(Int2IntMap.class);
	}

	@Override
	public Int2IntArrayMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final Int2IntMapSerializer.SerializationContainer container = p.readValueAs(Int2IntMapSerializer.SerializationContainer.class);

		final Int2IntArrayMap out = new Int2IntArrayMap(
				container.getKeys().stream().mapToInt(Integer::intValue).toArray(),
				container.getValues().stream().mapToInt(Integer::intValue).toArray()
		);

		out.defaultReturnValue(container.getDefaultValue());

		return out;
	}
}
