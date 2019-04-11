package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class IdReferenceSerializer extends StdSerializer<Identifiable> {

	private static final long serialVersionUID = 1L;

	public IdReferenceSerializer() {
		super(Identifiable.class);
	}
	
	@Override
	public void serialize(Identifiable value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeString(value.getId().toString());
	}
	
	@Override
	public void serializeWithType(Identifiable value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		serialize(value, gen, serializers);
	}
}
