package com.bakdata.conquery.models.index;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class MapIndexService implements Injectable {

	private final CsvParserSettings csvParserSettings;

	private final LoadingCache<Key, Map<String, String>> mappings = CacheBuilder.newBuilder().build(new CacheLoader<Key, Map<String, String>>() {
		@Override
		public Map<String, String> load(@NotNull Key key) throws Exception {

			final Map<String, String> int2ext = new HashMap<>();

			final CsvParser csvParser = new CsvParser(csvParserSettings);

			csvParser.beginParsing(key.csv.openStream());
			Record row;
			for (row = csvParser.parseNextRecord(); row != null; row = csvParser.parseNextRecord()) {
				final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

				substitutor.setEnableUndefinedVariableException(true);
				final String internalValue = row.getString(key.internalColumn);

				if (internalValue == null) {
					return null;
				}

				try {
					final String externalValue = substitutor.replace(key.externalTemplate);
					int2ext.put(internalValue, externalValue);
				}
				catch (IllegalArgumentException exception) {
					log.warn("Missing template values for line {}. Internal value: {}", csvParser.getContext()
																								 .currentLine(), internalValue, (Exception) (log.isTraceEnabled()
																																			 ? exception
																																			 : null));
				}

			}
			csvParser.stopParsing();

			return int2ext;
		}
	});


	public Map<String, String> getMapping(URL csv, String internalColumn, String externalTemplate) {
		try {
			return mappings.get(new Key(csv, internalColumn, externalTemplate));
		}
		catch (ExecutionException e) {
			throw new IllegalStateException(String.format("Unable to get mapping from %s (internal column = %s, external column = %s)", csv, internalColumn, externalTemplate), e);
		}
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MapIndexService.class, this);
	}

	@EqualsAndHashCode
	@RequiredArgsConstructor
	private static class Key {
		private final URL csv;
		private final String internalColumn;
		private final String externalTemplate;
	}
}
