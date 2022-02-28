package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.util.search.TrieSearch;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
public class FilterSearch {

	private final Map<String, TrieSearch<FEValue>> searchCache = new HashMap<>();

	public TrieSearch<FEValue> getSearchFor(String reference) {
		return searchCache.computeIfAbsent(reference, (ignored) -> new TrieSearch<>());
	}

	public boolean hasSearchFor(String reference) {
		return searchCache.containsKey(reference);
	}

	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(NamespacedStorage storage, JobManager jobManager, CSVConfig parser) {
		searchCache.clear();//TODO this is dangerous with shared cache

		storage.getAllConcepts().stream()
			   .flatMap(c -> c.getConnectors().stream())
			   .flatMap(co -> co.collectAllFilters().stream())
			   .filter(f -> f instanceof AbstractSelectFilter)
			   .map(AbstractSelectFilter.class::cast)
			   .map(f -> new SimpleJob(String.format("SourceSearch[%s]", f.getId()), () -> f.initializeSourceSearch(parser, storage, this)))
			   .forEach(jobManager::addSlowJob);
	}
}
