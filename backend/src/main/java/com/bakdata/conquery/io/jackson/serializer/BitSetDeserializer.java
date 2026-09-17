package com.bakdata.conquery.io.jackson.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class BitSetDeserializer extends StdDeserializer<BitSet> {
	protected BitSetDeserializer() {
		super(BitSet.class);
	}

	@Override
	public BitSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		try(final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			p.readBinaryValue(buffer);

			final byte[] bytes = buffer.toByteArray();
			return BitSet.valueOf(bytes);
		}
	}
}
