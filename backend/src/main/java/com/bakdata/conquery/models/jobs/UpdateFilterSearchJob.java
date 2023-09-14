package com.bakdata.conquery.models.jobs;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Slf4j
@RequiredArgsConstructor
public class UpdateFilterSearchJob extends Job {

	private final DistributedNamespace namespace;
	@NonNull
	private final NamespaceStorage storage;

	@NonNull
	private final Map<Searchable<?>, TrieSearch<FrontendValue>> searchCache;

	@NonNull
	private final IndexConfig indexConfig;

	@NonNull
	private final Object2LongMap<Searchable<?>> totals;

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


		final Set<Searchable<?>> collectedSearchables =
				allSelectFilters.stream()
								.map(SelectFilter::getSearchReferences)
								.flatMap(Collection::stream)
								// Disabling search is only a last resort for when columns are too big to store in memory or process for indexing.
								// TODO FK: We want no Searchable to be disabled, better scaling searches or mechanisms to fill search.
								.filter(Predicate.not(Searchable::isSearchDisabled))
								.collect(Collectors.toSet());


		// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
		final ExecutorService service = Executors.newCachedThreadPool();

		final Map<Searchable<?>, TrieSearch<FrontendValue>> synchronizedResult = Collections.synchronizedMap(searchCache);

		log.debug("Found {} searchable Objects.", collectedSearchables.size());

		for (Searchable<?> searchable : collectedSearchables) {
			if (searchable instanceof Column){
				// Handled on Shards
				continue;
			}

			service.submit(() -> {

				final StopWatch watch = StopWatch.createStarted();

				log.info("BEGIN collecting entries for `{}`", searchable.getId());

				try {
					final TrieSearch<FrontendValue> search = searchable.createTrieSearch(indexConfig, storage);

					if(search.findExact(List.of(""), 1).isEmpty()){
						search.addItem(new FrontendValue("", indexConfig.getEmptyLabel()), List.of(indexConfig.getEmptyLabel()));
					}

					synchronizedResult.put(searchable, search);

					log.debug(
							"DONE collecting entries for `{}`, within {}",
							searchable.getId(),
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

		namespace.matchingStatsManagerFinished();

	}

	@Override
	public String getLabel() {
		return "UpdateFilterSearchJob";
	}
}
