package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.BitSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class BitSetSerializer extends StdSerializer<BitSet> {
	protected BitSetSerializer() {
		super(BitSet.class);
	}

	@Override
	public void serialize(BitSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeBinary(value.toByteArray());
	}
}
