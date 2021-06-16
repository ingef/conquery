package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class IdReferenceKeySerializer extends StdSerializer<Identifiable> {
	protected IdReferenceKeySerializer() {
		super(Identifiable.class);
	}

	@Override
	public void serialize(Identifiable value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeFieldName(value.getId().toString());
	}
}
