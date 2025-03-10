package com.bakdata.conquery.models.jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.search.TrieSearch;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;

/**
 * Job that initializes the filter search for the frontend.
 * It collects all sources of values for all filters, e.g.:
 * <ul>
 *     <li>explicit mappings in a {@link SelectFilter}</li>
 *     <li>external reference mappings</li>
 *     <li>columns of imported data which are referenced by a filter</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateFilterSearchJob extends Job {

	private final Namespace namespace;

	@NonNull
	private final IndexConfig indexConfig;

	private final Consumer<Set<Column>> registerColumnValuesInSearch;

	@Override
	public void execute() throws Exception {

		final NamespaceStorage storage = namespace.getStorage();

		log.info("Clearing Search");
		namespace.getFilterSearch().clearSearch();


		log.info("BEGIN loading SourceSearch");

		// collect all SelectFilters to create searches for them
		final List<SelectFilter<?>> allSelectFilters =
				getAllSelectFilters(storage);


		// Unfortunately the is no ClassToInstanceMultimap yet
		final Map<Class<?>, Set<Searchable>> collectedSearchables =
				allSelectFilters.stream()
								.map(SelectFilter::getSearchReferences)
								.flatMap(Collection::stream)
								// Group Searchables into "Columns" and other "Searchables"
								.collect(Collectors.groupingBy(s -> s instanceof Column ? Column.class : Searchable.class, Collectors.toSet()));


		// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
		final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

		final Map<Searchable, TrieSearch<FrontendValue>> searchCache = new ConcurrentHashMap<>();

		log.debug("Found {} searchable Objects.", collectedSearchables.values().stream().mapToLong(Set::size).sum());

		for (Searchable searchable : collectedSearchables.getOrDefault(Searchable.class, Collections.emptySet())) {
			if (searchable instanceof Column) {
				throw new IllegalStateException("Columns should have been grouped out previously");
			}

			service.submit(() -> {

				final StopWatch watch = StopWatch.createStarted();

				log.info("BEGIN collecting entries for `{}`", searchable);

				try {
					final TrieSearch<FrontendValue> search = searchable.createTrieSearch(indexConfig);

					searchCache.put(searchable, search);

					log.debug(
							"DONE collecting {} entries for `{}`, within {}",
							search.calculateSize(),
							searchable,
							watch
					);
				}
				catch (Exception e) {
					log.error("Failed to create search for {}", searchable, e);
				}

			});
		}

		// The following cast is safe
		final Set<Column> searchableColumns = (Set) collectedSearchables.getOrDefault(Column.class, Collections.emptySet());
		log.debug("Start collecting column values: {}", Arrays.toString(searchableColumns.toArray()));
		registerColumnValuesInSearch.accept(searchableColumns);

		service.shutdown();


		while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
			if (getCancelledState().get()) {
				log.info("This job got canceled");
				service.shutdownNow();
				return;
			}
			log.debug("Still waiting for {} to finish.", Sets.difference(collectedSearchables.get(Searchable.class), searchCache.keySet()));
		}

		// Shrink searches before registering in the filter search
		searchCache.values().forEach(TrieSearch::shrinkToFit);

		namespace.getFilterSearch().addSearches(searchCache);

		log.info("UpdateFilterSearchJob search finished");

	}

	@NotNull
	public static List<SelectFilter<?>> getAllSelectFilters(NamespaceStorage storage) {
		try(Stream<Concept<?>> allConcepts = storage.getAllConcepts();) {
			return allConcepts
					.flatMap(c -> c.getConnectors().stream())
					.flatMap(co -> co.collectAllFilters().stream())
					.filter(SelectFilter.class::isInstance)
					.map(f -> ((SelectFilter<?>) f))
					.collect(Collectors.toList());
		}
	}

	@Override
	public String getLabel() {
		return "UpdateFilterSearchJob";
	}
}
