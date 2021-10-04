package com.bakdata.conquery.io.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.util.Arrays;

/**
 * This serializer avoids that the character sequence is transformed into an immutable String, with which we would lose control over invalidation of its content.
 */
public class CharArrayBufferSerializer extends JsonSerializer<CharArrayBuffer> {
	@Override
	public void serialize(CharArrayBuffer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		final char[] buffer = value.buffer();
		gen.writeString(buffer, 0, value.length());
		value.clear();
		Arrays.fill(buffer, '\0');
	}
}
