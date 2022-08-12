package com.bakdata.conquery.models.query;

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

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.SearchConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;


@Slf4j
@Data
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
	private Object2LongMap<SelectFilter<?>> totals = Object2LongMaps.emptyMap();

	/**
	 * From a given {@link FEValue} extract all relevant keywords.
	 */
	public static List<String> extractKeywords(FEValue value) {
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
					 .map(searchCache::get)
					 .filter(Objects::nonNull)
					 .collect(Collectors.toList());
	}

	public long getTotal(SelectFilter<?> filter) {
		return totals.getOrDefault(filter, 0);
	}


	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch() {

		totals = new Object2LongOpenHashMap<>();

		jobManager.addSlowJob(new UpdateFilterSearchJob(storage, searchCache, searchConfig, totals));
	}

}
