package com.bakdata.conquery.apiv1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.SearchConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Value
public class FilterSearch {

	private final NamespaceStorage storage;
	private final JobManager jobManager;
	private final CSVConfig parserConfig;
	private final SearchConfig searchConfig;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	private final Map<Searchable, TrieSearch<FEValue>> searchCache = new HashMap<>();

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
	 * For a {@link SelectFilter}, decide which references to use for searching.
	 *
	 * @implSpec the order defines the precedence in the output.
	 */
	private static List<Searchable> getSearchReferences(SelectFilter<?> filter) {
		final List<Searchable> references = new ArrayList<>(3);

		if (filter.getTemplate() != null) {
			references.add(filter.getTemplate().getSearchReference());
		}

		references.add(filter.getSearchReference());
		references.add(filter.getColumn().getSearchReference());

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

		jobManager.addSlowJob(new SimpleJob(
				"Initialize Source Search",
				() -> {

					log.info("BEGIN loading SourceSearch");

					// collect all SelectFilters to the create searches for them
					final List<SelectFilter<?>> allSelectFilters =
							storage.getAllConcepts().stream()
								   .flatMap(c -> c.getConnectors().stream())
								   .flatMap(co -> co.collectAllFilters().stream())
								   .filter(SelectFilter.class::isInstance)
								   .map(f -> ((SelectFilter<?>) f))
								   .collect(Collectors.toList());


					final Set<Searchable> suppliers = new HashSet<>();

					// collect all tasks that are based on the filters configured label mappings
					suppliers.addAll(collectLabelTasks(allSelectFilters));

					// collect all tasks based on the filters optionally configured templates based on csvs
					suppliers.addAll(collectTemplateTasks(allSelectFilters));

					// collect all tasks that are based on the raw data in the columns, these have no reference or template for mapping
					suppliers.addAll(collectColumnTasks(allSelectFilters));

					// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
					final ExecutorService service = Executors.newCachedThreadPool();

					final Map<Searchable, TrieSearch<FEValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

					log.debug("Found {} search suppliers", suppliers.size());

					for (Searchable searchProvider : suppliers) {

						service.submit(() -> {
							final Stream<FEValue> values = searchProvider.getSearchValues(getParserConfig(), storage);

							final long begin = System.currentTimeMillis();

							log.info("BEGIN collecting entries for `{}`", searchProvider);

							try {
								final TrieSearch<FEValue> search = new TrieSearch<>(
										searchProvider.isGenerateSuffixes() ? searchProvider.getMinSuffixLength() : Integer.MAX_VALUE,
										searchConfig.getSplit()
								);

								values.distinct()
									  .forEach(item -> search.addItem(item, extractKeywords(item)));

								search.shrinkToFit();

								synchronizedResult.put(searchProvider, search);

								final long end = System.currentTimeMillis();

								log.debug("DONE collecting entries for `{}`, within {} ({} Items)",
										  searchProvider,
										  Duration.ofMillis(end - begin), search.calculateSize()
								);
							}
							catch (Exception e) {
								log.error("Failed to create search for {}", searchProvider, e);
							}
						});
					}

					service.shutdown();


					while (!service.awaitTermination(30, TimeUnit.SECONDS)) {
						log.trace("Still waiting for {} to finish.", Sets.difference(synchronizedResult.keySet(), suppliers));
					}

					log.debug("DONE loading SourceSearch");
				}
		));


	}

	private Set<Searchable> collectColumnTasks(List<SelectFilter<?>> allSelectFilters) {
		final Set<Column> columns = allSelectFilters.stream().map(SingleColumnFilter::getColumn).collect(Collectors.toSet());

		return columns.stream()
					  .map(Column::getSearchReference)
					  .collect(Collectors.toSet());
	}

	private Set<Searchable> collectTemplateTasks(List<SelectFilter<?>> allSelectFilters) {
		return allSelectFilters.stream()
							   .filter(filter -> filter.getTemplate() != null)
							   .map(filter -> filter.getTemplate().getSearchReference())
							   .collect(Collectors.toSet());
	}

	private Set<Searchable> collectLabelTasks(List<SelectFilter<?>> allSelectFilters) {
		return allSelectFilters.stream()
							   .map(SelectFilter::getSearchReference)
							   .collect(Collectors.toSet());
	}

}
