package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class Int2ObjectMapDeserializer<T> extends StdDeserializer<Int2ObjectMap<T>> {

	public Int2ObjectMapDeserializer() {
		super(Int2ObjectMap.class);
	}


	@Override
	public Int2ObjectMap<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final Map<Integer, T> valueAs = p.readValueAs(Map.class);
		return new Int2ObjectOpenHashMap<T>(valueAs);
	}
}
