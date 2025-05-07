package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.search.InternalSearchConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.bakdata.conquery.util.search.Search;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.internal.Cursor;
import com.bakdata.conquery.util.search.internal.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;


@Slf4j
@RequiredArgsConstructor
public class InternalFilterSearch implements SearchProcessor {

	@Getter
	private final InternalSearchConfig searchConfig;
//	private final IndexConfig indexConfig;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	@JsonIgnore
	private ConcurrentMap<Searchable<FrontendValue>, TrieSearch<FrontendValue>> searchCache = new ConcurrentHashMap<>();
	private ConcurrentMap<SelectFilter<?>, Integer> totals = new ConcurrentHashMap<>();


	/**
	 * Cache of all search results on SelectFilters.
	 */
	private final LoadingCache<Pair<SelectFilter<?>, String>, List<FrontendValue>>
			searchResults =
			CacheBuilder.newBuilder().softValues().build(new CacheLoader<>() {

				@Override
				public List<FrontendValue> load(Pair<SelectFilter<?>, String> filterAndSearch) {
					final String searchTerm = filterAndSearch.getValue();
					final SelectFilter<?> searchable = filterAndSearch.getKey();

					log.trace("Calculating a new search cache for the term \"{}\" on Searchable[{}]", searchTerm, searchable.getId());

					return topItems(searchable, searchTerm);
				}

			});
	/**
	 * Cache of raw listing of values on a filter.
	 * We use Cursor here to reduce strain on memory and increase response time.
	 */
	private final LoadingCache<SelectFilter<?>, CursorAndLength> listResults = CacheBuilder.newBuilder().softValues().build(new CacheLoader<>() {
		@Override
		public CursorAndLength load(SelectFilter<?> searchable) {
			log.trace("Creating cursor for `{}`", searchable.getId());
			return new CursorAndLength(listAllValues(searchable), getTotal(searchable));
		}

	});

	/**
	 * From a given {@link FrontendValue} extract all relevant keywords.
	 */
	public static List<String> extractKeywords(FrontendValue value) {
		final List<String> keywords = new ArrayList<>(3);

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
	public final List<Search<FrontendValue>> getSearchesFor(SelectFilter<?> searchable) {
		final List<? extends Searchable<FrontendValue>> references = searchable.getSearchReferences();

		if (log.isTraceEnabled()) {
			log.trace("Got {} as searchables for {}", references.stream().map(Searchable::toString).collect(Collectors.toList()), searchable.getId());
		}

		return references.stream()
						 .map(searchCache::get)
						 .filter(Objects::nonNull)
						 .<Search<FrontendValue>>map(Search.class::cast)
						 .toList();
	}



	private Cursor<FrontendValue> listAllValues(SelectFilter<?> searchable) {
		/*
		Don't worry, I am as confused as you are!
		For some reason, flatMapped streams in conjunction with distinct will be evaluated full before further operation.
		This in turn causes initial loads of this endpoint to extremely slow. By instead using iterators we have uglier code but enforce laziness.

		See: https://stackoverflow.com/questions/61114380/java-streams-buffering-huge-streams
		 */

		final List<Search<FrontendValue>> searchList = getSearchesFor(searchable);

		final Iterator<FrontendValue> searches = Iterators.concat(Iterators.transform(searchList.iterator(), Search::iterator));
		final Iterator<FrontendValue> iterators =
				Iterators.concat(
						// We are always leading with the empty value.
						// Iterators.singletonIterator(new FrontendValue("", indexConfig.getEmptyLabel())),
						searches
				);

		// Use Set to accomplish distinct values
		final Set<FrontendValue> seen = new HashSet<>();

		return new Cursor<>(Iterators.filter(iterators, seen::add));
	}

	public long getTotal(SelectFilter<?> filter) {
		return totals.computeIfAbsent(filter, (f) -> {
			HashSet<FrontendValue> count = new HashSet<>();

			for (Search<FrontendValue> search : getSearchesFor(filter)) {
				search.iterator().forEachRemaining(count::add);
			}

			return count.size();
		});
	}


	/**
	 * Add ready searches to the cache. This assumes that the search already has been shrunken.
	 */
	public synchronized void addSearches(Map<Searchable<FrontendValue>, TrieSearch<FrontendValue>> searchCache) {

		this.searchCache.putAll(searchCache);
	}


	/**
	 * Adds new values to a search. If there is no search for the searchable yet, it is created.
	 * In order for this to work an existing search is not allowed to be shrunken yet, because shrinking
	 * prevents from adding new values.
	 */
	public void registerValues(Searchable<FrontendValue> searchable, Collection<String> values) {
		TrieSearch<FrontendValue> search = searchCache.computeIfAbsent(searchable, (ignored) -> searchConfig.createSearch(searchable));

		synchronized (search) {
			values.stream()
				  .map(value -> new FrontendValue(value, value))
				  .forEach(value -> search.addItem(value, extractKeywords(value)));
		}
	}

	/**
	 * Shrink the memory footprint of a search. After this action, no values can be registered anymore to a search.
	 */
	public void finalizeSearch(Searchable<FrontendValue> searchable) {
		final Search<FrontendValue> search = searchCache.get(searchable);

		if (search == null) {
			log.warn("Searchable has no search associated: {}", searchable);
			return;
		}
		search.finalizeSearch();
	}


	/**
	 * Autocompletion for search terms. For values of {@link SelectFilter <?>}.
	 * Is used by the search cache to load missing items
	 */
	public List<FrontendValue> topItems(SelectFilter<?> searchable, String text) {
		List<Search<FrontendValue>> searches = getSearchesFor(searchable);

		final Object2LongMap<FrontendValue> overlayedWeights = new Object2LongOpenHashMap<>();

		for (Search<FrontendValue> search : searches) {

			final Object2LongMap<FrontendValue> itemWeights = ((TrieSearch<FrontendValue>)search).collectWeights(List.of(text));

			itemWeights.forEach(overlayedWeights::putIfAbsent);
		}

		return TrieSearch.topItems(Integer.MAX_VALUE, overlayedWeights);
	}

	public synchronized void clearSearch() {
		totals = new ConcurrentHashMap<>();
		searchCache = new ConcurrentHashMap<>();
	}

	@Override
	public Job createUpdateFilterSearchJob(NamespaceStorage storage, Consumer<Set<Column>> columnsConsumer) {
		return new UpdateFilterSearchJob(storage, this, columnsConsumer);
	}

	public void indexManagerResidingSearches(Set<Searchable<FrontendValue>> managerSearchables, AtomicBoolean cancelledState, ProgressReporter progressReporter) throws InterruptedException {
		// Most computations are cheap but data intensive: we fork here to use as many cores as possible.
		try(final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)) {

			final Map<Searchable<FrontendValue>, TrieSearch<FrontendValue>> searchCache = new ConcurrentHashMap<>();
			for (Searchable<FrontendValue> searchable : managerSearchables) {
				if (searchable instanceof Column) {
					throw new IllegalStateException("Columns should have been grouped out previously");
				}

				service.submit(() -> {

					final StopWatch watch = StopWatch.createStarted();

					log.info("BEGIN collecting entries for `{}`", searchable);

					try {
						final TrieSearch<FrontendValue> search = searchConfig.createSearch(searchable);

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

			service.shutdown();


			while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
				if (cancelledState.get()) {
					log.info("This job got canceled");
					service.shutdownNow();
					return;
				}
				log.debug("Still waiting for {} to finish.", Sets.difference(managerSearchables, searchCache.keySet()));
			}

			// Shrink searches before registering in the filter search
			searchCache.values().forEach(Search::finalizeSearch);


			addSearches(searchCache);
		}
	}

	@Override
	public List<FrontendValue> findExact(SelectFilter<?> filter, String searchTerm) {

		final List<FrontendValue> out = new ArrayList<>();

		for (Search<FrontendValue> search : getSearchesFor(filter)) {
			List<FrontendValue> subResult = search.findExact(searchTerm, Integer.MAX_VALUE);
			out.addAll(subResult);
		}
		return out;
	}

	@Override
	public ConceptsProcessor.AutoCompleteResult query(SelectFilter<?> searchable, Optional<String> maybeText, int itemsPerPage, int pageNumber) {
		final int startIncl = itemsPerPage * pageNumber;
		final int endExcl = startIncl + itemsPerPage;

		try {

			// If we have none or a blank query string we list all values.
			if (maybeText.isEmpty() || maybeText.get().isBlank()) {
				final CursorAndLength cursorAndLength = listResults.get(searchable);
				final Cursor<FrontendValue> cursor = cursorAndLength.values();

				return new ConceptsProcessor.AutoCompleteResult(cursor.get(startIncl, endExcl), cursorAndLength.size());
			}

			final List<FrontendValue> fullResult = searchResults.get(Pair.of(searchable, maybeText.get()));

			if (startIncl >= fullResult.size()) {
				return new ConceptsProcessor.AutoCompleteResult(Collections.emptyList(), fullResult.size());
			}

			return new ConceptsProcessor.AutoCompleteResult(fullResult.subList(startIncl, Math.min(fullResult.size(), endExcl)), fullResult.size());
		}
		catch (ExecutionException e) {
			log.warn("Failed to search for \"{}\".", maybeText, (Exception) (log.isTraceEnabled() ? e : null));
			return new ConceptsProcessor.AutoCompleteResult(Collections.emptyList(), 0);
		}
	}



	/**
	 * Container class to pair number of available values and Cursor for those values.
	 */
	private record CursorAndLength(Cursor<FrontendValue> values, long size) {
	}
}
