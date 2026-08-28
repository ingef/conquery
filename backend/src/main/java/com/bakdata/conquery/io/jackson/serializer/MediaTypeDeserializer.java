package com.bakdata.conquery.io.jackson.serializer;

import jakarta.ws.rs.core.MediaType;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class MediaTypeDeserializer extends JsonDeserializer<MediaType> {
	@Override
	public MediaType deserialize(
		JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException {
		return MediaType.valueOf(jsonParser.getText());
	}
}
