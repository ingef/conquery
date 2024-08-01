package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.index.IndexCreationException;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class FilterSearch {

	@Getter
	private final IndexConfig indexConfig;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	@JsonIgnore
	private Map<Searchable, TrieSearch<FrontendValue>> searchCache = new HashMap<>();
	private Map<SelectFilter<?>, Integer> totals = new HashMap<>();

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

	public int getTotal(SelectFilter<?> filter) {
		return totals.computeIfAbsent(filter, (f) -> {
			HashSet<FrontendValue> count = new HashSet<>();

			for (TrieSearch<FrontendValue> search : getSearchesFor(filter)) {
				search.iterator().forEachRemaining(count::add);
			}

			return count.size();
		});
	}


	/**
	 * Add ready searches to the cache. This assumes that the search already has been shrunken.
	 */
	public synchronized void addSearches(Map<Searchable, TrieSearch<FrontendValue>> searchCache) {

		this.searchCache.putAll(searchCache);
	}


	/**
	 * Adds new values to a search. If there is no search for the searchable yet, it is created.
	 * In order for this to work an existing search is not allowed to be shrunken yet, because shrinking
	 * prevents from adding new values.
	 */
	public void registerValues(Searchable searchable, Collection<String> values) {
		TrieSearch<FrontendValue> search = searchCache.computeIfAbsent(searchable, (ignored) -> {
			try {
				return searchable.createTrieSearch(indexConfig);
			}
			catch (IndexCreationException e) {
				throw new IllegalStateException(e);
			}
		});

		synchronized (search) {
			values.stream()
				  .map(value -> new FrontendValue(value, value))
				  .forEach(value -> search.addItem(value, extractKeywords(value)));
		}
	}

	/**
	 * Shrink the memory footprint of a search. After this action, no values can be registered anymore to a search.
	 */
	public void shrinkSearch(Searchable searchable) {
		final TrieSearch<FrontendValue> search = searchCache.get(searchable);

		if (search == null) {
			log.warn("Searchable has no search associated: {}", searchable);
			return;
		}
		search.shrinkToFit();
	}

	public synchronized void clearSearch() {
		totals = new HashMap<>();
		searchCache = new HashMap<>();
	}
}
