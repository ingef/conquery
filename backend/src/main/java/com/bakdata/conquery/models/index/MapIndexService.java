package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MapIndexService implements Injectable {

	private final CsvParserSettings csvParserSettings;

	public MapIndexService(CsvParserSettings csvParserSettings) {
		this.csvParserSettings = csvParserSettings.clone();
		this.csvParserSettings.setHeaderExtractionEnabled(true);
	}

	private final Queue<CompletableFuture<Map<String, String>>> requestedMappings = new LinkedList<>();

	private final LoadingCache<Key, Map<String, String>> mappings = CacheBuilder.newBuilder().build(new CacheLoader<Key, Map<String, String>>() {
		@Override
		public Map<String, String> load(@NotNull Key key) throws Exception {
			log.info("Started to parse mapping {}", key);

			final Map<String, String> int2ext = new HashMap<>();

			final CsvParser csvParser = new CsvParser(csvParserSettings);

			try (InputStream inputStream = key.csv.openStream()) {

				final IterableResult<Record, ParsingContext> records = csvParser.iterateRecords(inputStream);

				// Set default to "" for all columns
				final String[] headers = records.getContext().headers();
				records.getContext().recordMetaData().setDefaultValueOfColumns("", headers);

				// Iterate records
				for (Record row : records) {
					final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

					final String internalValue = row.getString(key.internalColumn);

					if (internalValue == null) {
						log.trace("Could not create a mapping for row {} because the cell for the internal value was empty. Row: {}", csvParser.getContext()
																																			   .currentLine(),
								  log.isTraceEnabled()
								  ? StringUtils.join(row.toFieldMap())
								  : null
						);
						return null;
					}

					// We allow template values to be missing
					final String externalValue = substitutor.replace(key.externalTemplate);

					// Clean up the substitution by removing repeated white spaces
					String externalValueCleaned = externalValue.replaceAll("\\s+", " ");

					if (int2ext.containsKey(internalValue)) {
						log.warn(
								"Skipping mapping '{}'->'{}' in row {}, because there was already a mapping '{}'->'{}'",
								internalValue,
								externalValueCleaned,
								csvParser.getContext().currentLine(),
								internalValue,
								int2ext.get(internalValue)
						);
						continue;
					}

					int2ext.put(internalValue, externalValueCleaned);
				}
			}
			catch (IOException ioException) {
				log.warn("Failed to open {}", key.csv, ioException);
				throw ioException;
			}


			log.info("Finished parsing mapping {} with {} entries", key, int2ext.size());
			return int2ext;
		}
	});

	public void evictCache() {
		synchronized (requestedMappings) {
			// Invalidate all mapping requests by cancelling them
			final Iterator<CompletableFuture<Map<String, String>>> iterator = requestedMappings.iterator();

			while (iterator.hasNext()) {
				final CompletableFuture<Map<String, String>> next = iterator.next();

				next.obtrudeException(new CancellationException("The mapping for this future was evicted"));
				iterator.remove();

			}

			// Clear actual cache
			mappings.invalidateAll();
		}
	}


	public CompletableFuture<Map<String, String>> getMapping(URL csv, String internalColumn, String externalTemplate) {
		// TODO may use a specific executor service here
		synchronized (requestedMappings) {
			final CompletableFuture<Map<String, String>> mapCompletableFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return mappings.get(new Key(csv, internalColumn, externalTemplate));
				}
				catch (ExecutionException e) {
					throw new IllegalStateException(String.format("Unable to get mapping from %s (internal column = %s, external column = %s)", csv, internalColumn, externalTemplate), e);
				}
			});

			requestedMappings.add(mapCompletableFuture);

			return mapCompletableFuture;
		}
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(MapIndexService.class, this);
	}

	@EqualsAndHashCode
	@RequiredArgsConstructor
	@ToString
	private static class Key {
		private final URL csv;
		private final String internalColumn;
		private final String externalTemplate;
	}
}
