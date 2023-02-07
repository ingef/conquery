package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Uses the language tag provided by {@link Locale#toLanguageTag()} to serialize a locale as a string token.
 * Without this serializer Jackson would write out {@link Locale#GERMANY} as {@code \"de_DE\"}.
 * This serializer writes the locale as {@code de-DE}.
 */
public class LocaleSerializer extends JsonSerializer<Locale> {
	@Override
	public void serialize(Locale value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeString(value.toLanguageTag());
	}
}
