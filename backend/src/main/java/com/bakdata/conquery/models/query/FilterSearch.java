package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Functions;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class FilterSearch {

	private final IndexConfig indexConfig;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	@JsonIgnore
	private Map<Searchable, TrieSearch<FrontendValue>> searchCache = new HashMap<>();
	private Object2LongMap<Searchable> totals = Object2LongMaps.emptyMap();

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
	public final List<TrieSearch<FrontendValue>> getSearchesFor(SelectFilter<?> searchable) {
		final List<? extends Searchable> references = searchable.getSearchReferences();

		if (log.isTraceEnabled()) {
			log.trace("Got {} as searchables for {}", references.stream().map(Searchable::toString).collect(Collectors.toList()), searchable.getId());
		}

		return references.stream()
						 .map(searchCache::get)
						 .filter(Objects::nonNull)
						 .collect(Collectors.toList());
	}

	public long getTotal(SelectFilter<?> searchable) {
		return searchable.getSearchReferences().stream().mapToLong(totals::getLong).sum();
	}


	/**
	 * Add ready searches to the cache. This assumes that the search already has been shrunken.
	 */
	public synchronized void addSearches(Map<Searchable, TrieSearch<FrontendValue>> searchCache) {

		this.searchCache.putAll(searchCache);
		log.debug("BEGIN counting Search totals.");
		// Precompute totals as that can be slow when doing it on-demand.
		updateTotals(searchCache);
	}


	/**
	 * Adds new values to a search. If there is no search yet for the searchable, it is created.
	 * In order for this to work an existing search is not allowed to be shrunken yet, because shrinking
	 * prevents from adding new values.
	 */
	public void registerValues(Searchable searchable, Collection<String> values) {
		TrieSearch<FrontendValue> search = searchCache.computeIfAbsent(searchable, (ignored) -> searchable.createTrieSearch(indexConfig));

		synchronized (search) {
			values.stream()
				  .map(value -> new FrontendValue(value, value))
				  .forEach(value -> search.addItem(value, extractKeywords(value)));

			// Update totals for search
			totals.put(searchable, getTotalFor(searchable));
		}
	}

	private void updateTotals(Map<Searchable, TrieSearch<FrontendValue>> searchCache) {
		final Object2LongOpenHashMap<Searchable> partialTotals = new Object2LongOpenHashMap<>(searchCache.keySet().stream()
																											.collect(Collectors.toMap(
																													Functions.identity(),
																													this::getTotalFor
																											)));
		totals.putAll(partialTotals);
	}

	private long getTotalFor(Searchable searchable) {
		return searchCache.get(searchable).stream()
						  .mapToInt(FrontendValue::hashCode) // TODO Use distinct directly?
						  .distinct()
						  .count();
	}

	/**
	 * Shrink the memory footprint of a search. After this action, no values can be registered anymore to a search.
	 */
	public void shrinkSearch(Searchable searchable) {
		final TrieSearch<FrontendValue> search = getSearchCache().get(searchable);

		if (search == null) {
			log.warn("Searchable has no search associated: {}", searchable);
			return;
		}
		search.shrinkToFit();
	}

	public synchronized void clearSearch() {
		totals = new Object2LongOpenHashMap<>();
		searchCache.clear();
	}
}
