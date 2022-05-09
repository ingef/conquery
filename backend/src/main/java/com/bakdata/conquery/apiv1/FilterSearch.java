package com.bakdata.conquery.apiv1;

import java.time.Duration;
import java.util.Collection;
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

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.SearchConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
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
		return filter.getSearchReferences();
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


					final Set<Searchable> collectedSearchables =
							allSelectFilters.stream()
											.map(FilterSearch::getSearchReferences)
											.flatMap(Collection::stream)
											.collect(Collectors.toSet());


					// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
					final ExecutorService service = Executors.newCachedThreadPool();

					final Map<Searchable, TrieSearch<FEValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

					log.debug("Found {} searchable Objects.", collectedSearchables.size());

					for (Searchable searchable : collectedSearchables) {

						// Disabling search is only a last resort for when columns are too big to store in memory or process for indexing.
						// TODO FK: We want no Searchable to be disabled, better scaling searches or mechanisms to fill search.
						if(searchable.isSearchDisabled()){
							log.debug("{} is Disabled, skipping.", searchable);
							continue;
						}

						service.submit(() -> {
							final Stream<FEValue> values = searchable.getSearchValues(getParserConfig(), storage);

							final long begin = System.currentTimeMillis();

							log.info("BEGIN collecting entries for `{}`", searchable);

							try {
								final TrieSearch<FEValue> search = new TrieSearch<>(
										searchable.isGenerateSuffixes() ? searchable.getMinSuffixLength() : Integer.MAX_VALUE,
										searchConfig.getSplit()
								);

								values.distinct()
									  .forEach(item -> search.addItem(item, extractKeywords(item)));

								search.shrinkToFit();

								synchronizedResult.put(searchable, search);

								final long end = System.currentTimeMillis();

								log.debug("DONE collecting entries for `{}`, within {} ({} Items)",
										  searchable,
										  Duration.ofMillis(end - begin), search.calculateSize()
								);
							}
							catch (Exception e) {
								log.error("Failed to create search for {}", searchable, e);
							}
						});
					}

					service.shutdown();


					while (!service.awaitTermination(30, TimeUnit.SECONDS)) {
						log.debug("Still waiting for {} to finish.", Sets.difference(collectedSearchables, synchronizedResult.keySet()));
					}

					log.debug("DONE loading SourceSearch");
				}
		));
	}

}
