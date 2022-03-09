package com.bakdata.conquery.apiv1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
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
	public void updateSearch(NamespaceStorage storage, JobManager jobManager, CSVConfig parser) {
		final Map<String, Supplier<TrieSearch<FEValue>>> suppliers =
				storage.getAllConcepts().stream()
					   .flatMap(c -> c.getConnectors().stream())
					   .flatMap(co -> co.collectAllFilters().stream())
					   .filter(f -> f instanceof AbstractSelectFilter)
					   .map(f -> ((AbstractSelectFilter<?>) f))
					   .map(f -> f.initializeSourceSearch(parser, storage, this))

					   .map(Map::entrySet)
					   .flatMap(Collection::stream)
					   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {
			ExecutorService service = Executors.newCachedThreadPool();

			suppliers.forEach((id, supplier) -> {
				service.submit(() -> {
					final TrieSearch<FEValue> search = supplier.get();
					searchCache.put(id, search);

					log.info("Stats for `{}`", id);
					search.logStats();
				});
			});

			service.shutdown();

			service.awaitTermination(10, TimeUnit.HOURS);
		}));


	}
}
