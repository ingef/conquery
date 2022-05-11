package com.bakdata.conquery.apiv1;

import java.time.Duration;
import java.util.ArrayList;
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
import java.util.function.Predicate;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;


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
	@JsonIgnore
	private final Map<Searchable, TrieSearch<FEValue>> searchCache = new HashMap<>();

	/**
	 * From a given {@link FEValue} extract all relevant keywords.
	 */
	private static List<String> extractKeywords(FEValue value) {
		List<String> keywords = new ArrayList<>(3);

		keywords.add(value.getLabel());
		keywords.add(value.getValue());

		if (value.getOptionValue() != null) {
			keywords.add(value.getOptionValue());
		}

		return keywords;
	}

	/**
	 * For a {@link SelectFilter} collect all relevant {@link TrieSearch}.
	 */
	public List<TrieSearch<FEValue>> getSearchesFor(SelectFilter<?> filter) {
		return filter.getSearchReferences().stream()
					 .filter(Predicate.not(Searchable::isSearchDisabled))
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
											.map(SelectFilter::getSearchReferences)
											.flatMap(Collection::stream)
											// Disabling search is only a last resort for when columns are too big to store in memory or process for indexing.
											// TODO FK: We want no Searchable to be disabled, better scaling searches or mechanisms to fill search.
											.filter(Predicate.not(Searchable::isSearchDisabled))
											.collect(Collectors.toSet());


					// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
					final ExecutorService service = Executors.newCachedThreadPool();

					final Map<Searchable, TrieSearch<FEValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

					log.debug("Found {} searchable Objects.", collectedSearchables.size());

					for (Searchable searchable : collectedSearchables) {

						service.submit(() -> {
							final Stream<FEValue> values = searchable.getSearchValues(getParserConfig(), storage);

							final StopWatch watch = StopWatch.createStarted();

							log.info("BEGIN collecting entries for `{}`", searchable);

							try {
								final TrieSearch<FEValue> search = new TrieSearch<>(
										searchable.isGenerateSuffixes() ? searchable.getMinSuffixLength() : Integer.MAX_VALUE,
										searchConfig.getSplit()
								);

								values.distinct()
									  .forEach(item -> search.addItem(item, extractKeywords(item)));

								search.shrinkToFit();

								if (log.isDebugEnabled()) {
									log.debug(
											"DONE collecting entries for `{}`, within {} ({} Items)",
											searchable,
											Duration.ofMillis(watch.getTime()),
											search.calculateSize()
									);
								}

								synchronizedResult.put(searchable, search);

							}
							catch (Exception e) {
								log.error("Failed to create search for {}", searchable, e);
							}
						});
					}

					service.shutdown();


					while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
						log.debug("Still waiting for {} to finish.", Sets.difference(collectedSearchables, synchronizedResult.keySet()));
					}

					log.debug("DONE loading SourceSearch");
				}
		));
	}

}
