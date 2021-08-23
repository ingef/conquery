package com.bakdata.conquery.io.jackson.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class MediaTypeDeserializer extends JsonDeserializer<MediaType> {
	@Override
	public MediaType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		return MediaType.valueOf(jsonParser.getText());
	}
}
