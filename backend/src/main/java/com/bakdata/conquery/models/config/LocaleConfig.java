package com.bakdata.conquery.models.config;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LocaleConfig {
	@NotNull
	private Locale frontend = Locale.ROOT;

	@NotNull
	@JsonDeserialize(contentUsing = DateTimeFormatterDeserializer.class)
	private Map<Locale, DateTimeFormatter> dateFormatMapping = Collections.emptyMap();

	public static class DateTimeFormatterDeserializer extends JsonDeserializer<DateTimeFormatter> {

		@Override
		public DateTimeFormatter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			return DateTimeFormatter.ofPattern(p.getText());
		}
	}
}
