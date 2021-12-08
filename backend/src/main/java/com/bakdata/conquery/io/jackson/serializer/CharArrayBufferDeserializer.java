package com.bakdata.conquery.io.jackson.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.util.Arrays;

/**
 * This custom serializer treats an incoming text node as a mutable string, which is important for sensitve data.
 * Doing so, allows us to clear buffers. Since {@link String} is immutable it is inappropriate.
 */
public class CharArrayBufferDeserializer extends JsonDeserializer<CharArrayBuffer> {
	@Override
	public CharArrayBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final JsonToken currentToken = p.getCurrentToken();
		if(currentToken != JsonToken.VALUE_STRING){
			ctxt.handleUnexpectedToken(CharArrayBuffer.class, currentToken, p,"cannot deserialize CharArrayBuffer");
		}
		final char[] chars = p.readValueAs(char[].class);
		final CharArrayBuffer charArrayBuffer = new CharArrayBuffer(chars.length);
		charArrayBuffer.append(chars, 0, chars.length);
		Arrays.fill(chars, '\0');
		return charArrayBuffer;
	}
}
