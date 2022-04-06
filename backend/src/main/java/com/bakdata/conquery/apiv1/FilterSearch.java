package com.bakdata.conquery.apiv1;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.collect.ImmutableList;
import com.univocity.parsers.csv.CsvParser;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;


@Slf4j
@Value
public class FilterSearch {

	private final NamespaceStorage storage;
	private final JobManager jobManager;
	private final CSVConfig parser;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	private final Map<String, TrieSearch<FEValue>> searchCache = new HashMap<>();

	/**
	 * From a given {@link FEValue} extract all relevant keywords.
	 */
	private static List<String> extractKeywords(FEValue value) {
		final ImmutableList.Builder<String> builder = ImmutableList.builderWithExpectedSize(3);

		builder.add(value.getLabel())
			   .add(value.getValue());

		if (value.getOptionValue() != null) {
			builder.add(value.getOptionValue());
		}

		return builder.build();
	}

	/**
	 * To facilitate sharing/reuse within a dataset, we try to combine multiple columns into a single search:
	 * If the column is part of a shared-Dictionary, we use that as reference.
	 * Usually {@link Column}s of a {@link com.bakdata.conquery.models.datasets.SecondaryIdDescription} is also backed by a shared-Dictionary, but in the case it is not, we use the secondaryId as common reference name.
	 * <p>
	 * Lastly if the column is not shared or of a secondaryId we use the {@link com.bakdata.conquery.models.identifiable.ids.specific.ColumnId} itself.
	 */
	private static String decideColumnReference(Column column) {

		if (column.getSharedDictionary() != null) {
			return column.getSharedDictionary();
		}

		if (column.getSecondaryId() != null) {
			return column.getSecondaryId().getId().toString();
		}

		return column.getId().toString();
	}

	/**
	 * For a {@link SelectFilter}, decide which references to use for searching.
	 */
	private static List<String> getSearchReferences(SelectFilter<?> filter) {
		final List<String> references = new ArrayList<>(3);

		if (filter.getTemplate() != null) {
			// TODO when templates have Ids use those, but until then we use the hashcode because some files might be used multiple times
			references.add(Integer.toString(filter.getTemplate().hashCode()));
		}

		references.add(filter.getId().toString());
		references.add(decideColumnReference(filter.getColumn()));

		return references;
	}

	/**
	 * For a {@link SelectFilter} collect all relevant {@link TrieSearch}.
	 */
	public List<TrieSearch<FEValue>> getSearchesFor(SelectFilter<?> filter) {
		return getSearchReferences(filter).stream()
										  .map(searchCache::get)
										  .filter(Objects::nonNull)
										  .collect(Collectors.toList());
	}


	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch() {

		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {

			log.info("BEGIN loading SourceSearch");

			// collect all SelectFilters to the create searches for them
			final List<SelectFilter<?>> allSelectFilters =
					storage.getAllConcepts().stream()
						   .flatMap(c -> c.getConnectors().stream())
						   .flatMap(co -> co.collectAllFilters().stream())
						   .filter(SelectFilter.class::isInstance)
						   .map(f -> ((SelectFilter<?>) f))
						   .collect(Collectors.toList());


			final Map<String, Stream<FEValue>> suppliers = new HashMap<>();

			// collect all tasks that are based on the filters configured label mappings
			collectLabelTasks(allSelectFilters, suppliers);

			// collect all tasks based on the filters optionally configured templates based on csvs
			collectTemplateTasks(parser, allSelectFilters, suppliers);

			// collect all tasks that are based on the raw data in the columns, these have no reference or template for mapping
			collectColumnTasks(storage, allSelectFilters, suppliers);

			// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
			final ExecutorService service = Executors.newCachedThreadPool();

			final Map<String, TrieSearch<FEValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

			log.debug("Found {} search suppliers", suppliers.size());

			for (Map.Entry<String, Stream<FEValue>> entry : suppliers.entrySet()) {

				service.submit(() -> {
					final String id = entry.getKey();
					final Stream<FEValue> values = entry.getValue();

					final long begin = System.currentTimeMillis();

					log.info("BEGIN collecting entries for `{}`", id);

					try {
						final TrieSearch<FEValue> search = new TrieSearch<>();

						values.distinct()
							  .forEach(item -> search.addItem(item, extractKeywords(item)));

						search.shrinkToFit();

						synchronizedResult.put(id, search);

						final long end = System.currentTimeMillis();

						log.debug("DONE collecting entries for `{}`, within {} ({} Items)", id, Duration.ofMillis(end - begin), search.calculateSize());
					}
					catch (Exception e) {
						log.error("Failed to create search for {}", id, e);
					}
				});
			}

			service.shutdown();


			while (!service.awaitTermination(30, TimeUnit.SECONDS)) {
				log.trace("Still waiting for {} to finish.", suppliers.size() - searchCache.size());
			}

			log.debug("DONE loading SourceSearch");
		}));


	}

	/**
	 * Create Streams extracting data from raw data in columns.
	 */
	private void collectColumnTasks(NamespaceStorage storage, List<SelectFilter<?>> allSelectFilters, Map<String, Stream<FEValue>> suppliers) {
		final Set<Column> columns = allSelectFilters.stream().map(SingleColumnFilter::getColumn).collect(Collectors.toSet());

		for (Column column : columns) {
			final List<Import> imports = column.getTable().findImports(storage).collect(Collectors.toList());
			final String reference = decideColumnReference(column);

			final Stream<FEValue> fromColumn =
					imports.stream()
						   .flatMap(imp -> StreamSupport.stream(((StringStore) column.getTypeFor(imp)).spliterator(), false))
						   .map(value -> new FEValue(value, value))
						   .onClose(() -> log.debug("DONE processing values for {}", column.getId()));

			// Data for columns can grouped by secondaryId or sharedDict therefore we have to concatenate them instead of simply supplying them
			// We group the values because it drastically reduces memory usage, shared dicts and secondaryids mostly contain the same data over a lot of columns
			final Stream<FEValue> prior = suppliers.getOrDefault(reference, Stream.empty());
			suppliers.put(reference, Stream.concat(prior, fromColumn));

		}
	}

	private static Stream<FEValue> fromTemplate(FilterTemplate template, CSVConfig parserConfig) {
		final CsvParser parser = parserConfig.createParser();
		// It is likely that multiple Filters reference the same file+config. However we want to ensure it is read only once to avoid wasting computation.
		// We use Streams below to ensure a completely transparent lazy execution of parsing reference files
		return Stream.of(new File(template.getFilePath()))
					 .map(parser::iterateRecords)
					 // Univocity parser does not support streams, so we create one manually using their spliterator.
					 .flatMap(iter -> StreamSupport.stream(iter.spliterator(), false))
					 .map(row -> {
						 final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

						 substitutor.setEnableUndefinedVariableException(true);
						 final String rowId = row.getString(template.getColumnValue());

						 if (rowId == null) {
							 return null;
						 }

						 try {
							 final String label = substitutor.replace(template.getValue());
							 final String optionValue = substitutor.replace(template.getOptionValue());

							 return new FEValue(rowId, label, optionValue);
						 }
						 catch (IllegalArgumentException exception) {
							 log.warn("Missing template values for line `{}`", rowId, (Exception) (log.isTraceEnabled() ? exception : null));
							 return null;
						 }
					 })
					 .filter(Objects::nonNull)
					 .distinct();
	}

	/**
	 * Create streams for all templates referenced in the filters
	 */
	private void collectTemplateTasks(CSVConfig parser, List<SelectFilter<?>> allSelectFilters, Map<String, Stream<FEValue>> suppliers) {
		for (SelectFilter<?> filter : allSelectFilters) {
			if (filter.getTemplate() == null) {
				continue;
			}

			final String reference = Integer.toString(filter.getTemplate().hashCode());

			if (suppliers.containsKey(reference)) {
				continue;
			}
			//TODO FK: if templates have proper Ids we can make this more stringent (also the search references)

			suppliers.put(reference, fromTemplate(filter.getTemplate(), parser));
		}
	}

	/**
	 * Create Streams extracting data from filter labels
	 */
	private void collectLabelTasks(List<SelectFilter<?>> allSelectFilters, Map<String, Stream<FEValue>> suppliers) {
		for (SelectFilter<?> filter : allSelectFilters) {
			if (filter.getLabels().isEmpty()) {
				continue;
			}
			final Map<String, String> labels = filter.getLabels();

			suppliers.put(
					filter.getId().toString(),
					labels.entrySet().stream()
						  .map(entry -> new FEValue(entry.getKey(), entry.getValue()))
						  .onClose(() -> log.debug("DONE processing {} labels for {}", labels.size(), filter.getId()))
			);

		}
	}
}
