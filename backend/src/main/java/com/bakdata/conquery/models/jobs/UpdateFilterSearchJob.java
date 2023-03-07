package com.bakdata.conquery.models.jobs;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Slf4j
@RequiredArgsConstructor
public class UpdateFilterSearchJob extends Job {
	@NonNull
	private final NamespaceStorage storage;

	@NonNull
	private final Map<Searchable, TrieSearch<FrontendValue>> searchCache;

	@NonNull
	private final IndexConfig indexConfig;

	@NonNull
	private final Object2LongMap<Searchable> totals;

	@Override
	public void execute() throws Exception {


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

		final Map<Searchable, TrieSearch<FrontendValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

		log.debug("Found {} searchable Objects.", collectedSearchables.size());


		for (Searchable searchable : collectedSearchables) {

			service.submit(() -> {

				final StopWatch watch = StopWatch.createStarted();

				log.info("BEGIN collecting entries for `{}`", searchable);

				try {
					final List<TrieSearch<FrontendValue>> values = searchable.getSearches(indexConfig, storage);

					for (TrieSearch<FrontendValue> search : values) {
						synchronizedResult.put(searchable, search);
					}

					log.debug(
							"DONE collecting entries for `{}`, within {}",
							searchable,
							Duration.ofMillis(watch.getTime())
					);
				}
				catch (Exception e) {
					log.error("Failed to create search for {}", searchable, e);
				}

			});
		}

		service.shutdown();


		while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
			if (getCancelledState().get()) {
				log.info("This job got canceled");
				service.shutdownNow();
				return;
			}
			log.debug("Still waiting for {} to finish.", Sets.difference(collectedSearchables, synchronizedResult.keySet()));
		}

		log.debug("BEGIN counting Search totals.");


		// Precompute totals as that can be slow when doing it on-demand.
		totals.putAll(
				synchronizedResult.keySet()
								  .parallelStream()
								  .collect(Collectors.toMap(
										  Functions.identity(),
										  filter -> filter.getSearchReferences().stream()
														  .map(searchCache::get)
														  .filter(Objects::nonNull) // Failed or disabled searches are null
														  .flatMap(TrieSearch::stream)
														  .mapToInt(FrontendValue::hashCode)
														  .distinct()
														  .count()
								  ))
		);


		log.debug("DONE loading SourceSearch");
	}

	@Override
	public String getLabel() {
		return "UpdateFilterSearchJob";
	}
}
