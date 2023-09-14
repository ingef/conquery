package com.bakdata.conquery.models.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.IndexConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
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

	private final NamespaceStorage storage;
	private final JobManager jobManager;
	private final CSVConfig parserConfig;
	private final IndexConfig indexConfig;

	/**
	 * We tag our searches based on references collected in getSearchReferences. We do not mash them all together to allow for sharing and prioritising different sources.
	 * <p>
	 * In the code below, the keys of this map will usually be called "reference".
	 */
	@JsonIgnore
	private final Map<Searchable<?>, TrieSearch<FrontendValue>> searchCache = new HashMap<>();
	private Object2LongMap<Searchable<?>> totals = Object2LongMaps.emptyMap();

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
	public final List<TrieSearch<FrontendValue>> getSearchesFor(Searchable<?> searchable) {
		final List<Searchable<?>> references = searchable.getSearchReferences();

		if(log.isTraceEnabled()) {
			log.trace("Got {} as searchables for {}", references.stream().map(Searchable::getId).collect(Collectors.toList()), searchable.getId());
		}

		return references.stream()
						 .map(searchCache::get)
						 .filter(Objects::nonNull)
						 .collect(Collectors.toList());
	}

	public long getTotal(Searchable<?> searchable) {
		return totals.getOrDefault(searchable, 0);
	}


	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch() {

		totals = new Object2LongOpenHashMap<>();

		jobManager.addSlowJob(new UpdateFilterSearchJob(storage, searchCache, indexConfig, totals));
	}


	public void registerValues(Column column, Collection<String> values) {
		TrieSearch<FrontendValue> search = searchCache.computeIfAbsent(column, (ignored) -> column.createTrieSearch(indexConfig, storage));

		synchronized (search) {
			values.stream()
				  .map(value -> new FrontendValue(value, value))
				  .forEach(value -> search.addItem(value, extractKeywords(value)));
		}
	}

	public void calculateTotals() {
		final Object2LongOpenHashMap<Searchable<?>> newTotals =
				new Object2LongOpenHashMap<>(searchCache.keySet().stream()
														.collect(Collectors.toMap(
																Functions.identity(),
																filter -> filter.getSearchReferences()
																				.stream()
																				.map(searchCache::get)
																				.filter(Objects::nonNull) // Failed or disabled searches are null
																				.flatMap(TrieSearch::stream)
																				.mapToInt(FrontendValue::hashCode)
																				.distinct()
																				.count()
														)));

		setTotals(newTotals);
	}

	public void shrinkSearches() {
		final Map<Searchable<?>, TrieSearch<FrontendValue>> searchCache = getSearchCache();

		searchCache.values().forEach(TrieSearch::shrinkToFit);
	}
}
