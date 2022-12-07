package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class Int2ObjectMapSerializer extends StdSerializer<Int2ObjectMap> {

	public Int2ObjectMapSerializer() {
		super(Int2ObjectMap.class);
	}



	@Override
	public void serialize(Int2ObjectMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		provider.findValueSerializer(Map.class).serialize(value, gen, provider);
	}
}
