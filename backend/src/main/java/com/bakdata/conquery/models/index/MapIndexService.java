package com.bakdata.conquery.models.index;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class MapIndexService implements Injectable {

	private final CsvParserSettings csvParserSettings;

	private final LoadingCache<Key, Map<String, String>> mappings = CacheBuilder.newBuilder().build(new CacheLoader<Key, Map<String, String>>() {
		@Override
		public Map<String, String> load(@NotNull Key key) throws Exception {

			final Map<String, String> int2ext = new HashMap<>();

			csvParserSettings.selectFields(key.internalColumn, key.externalColumn);
			RowProcessor processor = new RowProcessor() {

				@Override
				public void processStarted(ParsingContext context) {
					// get header indexes
					//					final List<String> headers = Arrays.asList(context.headers());
					//					intColIndex = headers.indexOf(key.internalColumn);
					//					if (intColIndex < 0) {
					//						throw new IllegalStateException(String.format("File %s has no column %s", key.csv, key.internalColumn));
					//					}
					//					extColIndex = headers.indexOf(key.externalColumn);
					//					if (extColIndex < 0) {
					//						throw new IllegalStateException(String.format("File %s has no column %s", key.csv, key.externalColumn));
					//					}
					// nothing to do; the fields are already selected
				}

				@Override
				public void rowProcessed(String[] row, ParsingContext context) {
					// use the first two cells as we selected fields
					log.trace("Processing {}#{}: internal value '{}'; external value '{}' from ", key.csv, context.currentLine(), row[0], row[1]);
					int2ext.put(row[0], row[1]);

				}

				@Override
				public void processEnded(ParsingContext context) {
					log.trace("Processed {} lines from {}", context.currentLine(), key.csv);
				}
			};
			csvParserSettings.setProcessor(processor);
			final CsvParser csvParser = new CsvParser(csvParserSettings);

			csvParser.beginParsing(key.csv.openStream());
			while (csvParser.parseNext() != null) {
			}
			csvParser.stopParsing();

			return int2ext;
		}
	});


	public Map<String, String> getMapping(URL csv, String internalColumn, String externalColumn) {
		try {
			return mappings.get(new Key(csv, internalColumn, externalColumn));
		}
		catch (ExecutionException e) {
			throw new IllegalStateException(String.format("Unable to get mapping from %s (internal column = %s, external column = %s)", csv, internalColumn, externalColumn), e);
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
		private final String externalColumn;
	}
}
