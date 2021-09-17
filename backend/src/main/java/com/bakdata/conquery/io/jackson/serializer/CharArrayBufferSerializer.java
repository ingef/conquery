package com.bakdata.conquery.io.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;

public class CharArrayBufferSerializer extends JsonSerializer<CharArrayBuffer> {
	@Override
	public void serialize(CharArrayBuffer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		//TODO don't use String here
		gen.writeString(value.toString());
	}
}
