package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses a language tag as a locale using {@link Locale#forLanguageTag(String)}.
 * Fails if the provided token is not a string token or the tag cannot be matched to a locale.
 */
public class LocaleDeserializer extends JsonDeserializer<Locale> {
	@Override
	public Locale deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		final JsonToken jsonToken = p.currentToken();
		if (jsonToken != JsonToken.VALUE_STRING) {
			throw MismatchedInputException.from(p, Locale.class, "Expected a string token but found: " + jsonToken);
		}

		final String languageTag = p.getText();
		final Locale locale = Locale.forLanguageTag(languageTag);

		if (StringUtils.isNotBlank(languageTag) && !languageTag.equals(Locale.ROOT.toLanguageTag()) && locale == Locale.ROOT) {
			// When Locale#forLanguageTag does not recognize the tag it defaults to Locale.ROOT
			// This should only be intended if the tag was blank or "und" (undefined ~= Locale.ROOT.toLanguageTag())
			throw MismatchedInputException.from(p, Locale.class, "Unable to language tag '" + languageTag + "' to locale");
		}

		return locale;
	}
}
