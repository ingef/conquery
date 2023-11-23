package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.google.common.base.CharMatcher;
import com.google.common.base.Functions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A service that provides indexes for given {@link IndexKey}s.
 * An index is created upon first request and cached.
 */
@Slf4j
public class IndexService implements Injectable {

	private final CsvParserSettings csvParserSettings;

	private final LoadingCache<IndexKey<?>, Index<?>> mappings = CacheBuilder.newBuilder().recordStats().build(new CacheLoader<>() {
		@Override
		public Index<?> load(@NotNull IndexKey<?> key) throws Exception {
			log.info("Started to parse mapping {}", key);

			final Map<String, String> emptyDefaults = computeEmptyDefaults(key);

			final Index<?> int2ext = key.createIndex();

			final CsvParser csvParser = new CsvParser(csvParserSettings);

			try (InputStream inputStream = key.getCsv().toURL().openStream()) {

				final IterableResult<Record, ParsingContext> records = csvParser.iterateRecords(inputStream);

				// Set default to "" for all columns
				final String[] headers = records.getContext().headers();
				records.getContext().recordMetaData().setDefaultValueOfColumns("", headers);

				// Iterate records
				for (Record row : records) {
					final Pair<String, Map<String, String>> pair = computeInternalExternal(key, csvParser, row);
					if (pair == null) {
						continue;
					}

					final String internalValue = pair.getLeft();
					final Map<String, String> externalValue = pair.getRight();

					// If the computed value is equal to a template without any values, replace it with value
					externalValue.replaceAll((template, value) -> emptyDefaults.get(template).equals(value) ? internalValue : value);

					try {
						int2ext.put(internalValue, externalValue);
					}
					catch (IllegalArgumentException e) {
						log.warn("Skipping mapping '{}'->'{}' in row {}, because there was already a mapping",
								 internalValue, externalValue, csvParser.getContext().currentLine(),
								 (Exception) (log.isTraceEnabled() ? e : null) // Cast to Exception to satisfy format-string check
						);
					}
				}
			}
			catch (IOException ioException) {
				log.warn("Failed to open `{}`", key.getCsv(), ioException);
				throw ioException;
			}

			log.info("Finished parsing mapping {} with {} entries", key, int2ext.size());

			// Run finalizing operations on the index
			int2ext.finalizer();

			return int2ext;
		}
	});

	public IndexService(CsvParserSettings csvParserSettings) {
		this.csvParserSettings = csvParserSettings.clone();
		this.csvParserSettings.setHeaderExtractionEnabled(true);
	}

	@Nullable
	private Pair<String, Map<String, String>> computeInternalExternal(@NotNull IndexKey<?> key, CsvParser csvParser, Record row) {
		final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

		final String internalValue = row.getString(key.getInternalColumn());

		if (internalValue == null) {
			log.trace("Could not create a mapping for row {} because the cell for the internal value was empty. Row: {}", csvParser.getContext().currentLine(),
					  log.isTraceEnabled()
					  ? StringUtils.join(row.toFieldMap())
					  : null
			);
			return null;
		}

		final List<String> externalTemplates = key.getExternalTemplates();

		final Map<String, String> templateToConcrete = computeTemplates(substitutor, externalTemplates);

		return Pair.of(internalValue, templateToConcrete);
	}

	@NotNull
	private Map<String, String> computeTemplates(StringSubstitutor substitutor, List<String> externalTemplates) {
		final CharMatcher whitespaceMatcher = CharMatcher.whitespace();

		return externalTemplates.stream()
								.distinct()
								.collect(Collectors.toMap(Functions.identity(), value -> whitespaceMatcher.trimAndCollapseFrom(substitutor.replace(value), ' ')));
	}

	private Map<String, String> computeEmptyDefaults(IndexKey<?> key) {
		final StringSubstitutor substitutor = new StringSubstitutor((ignored) -> "", "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

		final List<String> externalTemplates = key.getExternalTemplates();

		return computeTemplates(substitutor, externalTemplates);
	}

	public void evictCache() {
		mappings.invalidateAll();
	}


	@SuppressWarnings("unchecked")
	public <K extends IndexKey<I>, I extends Index<K>> I getIndex(K key) {
		try {
			return (I) mappings.get(key);
		}
		catch (ExecutionException e) {
			throw new IllegalStateException(String.format("Unable to build index from index configuration: %s)", key), e);
		}
	}

	public CacheStats getStatistics() {
		return mappings.stats();
	}

	public Set<IndexKey<?>> getLoadedIndexes() {
		return mappings.asMap().keySet();
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(IndexService.class, this);
	}
}
