package com.bakdata.conquery.util;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Mappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class PrettyPrinter {

	private static final Indenter INDENTER = new DefaultIndenter("\t", "\n");
	private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter() {
		private static final long serialVersionUID = 1L;

		@Override
		public DefaultPrettyPrinter withSeparators(Separators separators) {
			_separators = separators;
			_objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
			return this;
		}
	}
			.withSeparators(new Separators())
			.withArrayIndenter(INDENTER)
			.withObjectIndenter(INDENTER)
			.withSpacesInObjectEntries();

	private static final ObjectMapper mapper = Mappers.getMapper();

	public static String print(String json) throws JsonProcessingException, IOException {
		try {
			return mapper
						  .setDefaultPrettyPrinter(PRETTY_PRINTER)
						  .writerWithDefaultPrettyPrinter()
						  .writeValueAsString(mapper.readTree(json));
		}
		catch (Exception e) {
			log.warn("Could not pretty print:\n{}", json);
			return json;
		}
	}
}
