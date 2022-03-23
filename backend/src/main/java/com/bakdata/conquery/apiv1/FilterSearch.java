package com.bakdata.conquery.apiv1;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		return searchCache.getOrDefault(reference, new TrieSearch<>());
	}

	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(NamespaceStorage storage, JobManager jobManager, CSVConfig parser) {

		jobManager.addSlowJob(new SimpleJob("Initialize Source Search", () -> {

			log.info("BEGIN loading SourceSearch");


			final Stream<AbstractSelectFilter<?>> allSelectFilters =
					storage.getAllConcepts().stream()
						   .flatMap(c -> c.getConnectors().stream())
						   .flatMap(co -> co.collectAllFilters().stream())
						   .filter(AbstractSelectFilter.class::isInstance)
						   .map(AbstractSelectFilter.class::cast);

			// Generate SourceSearchTasks, then group them by their targetId (i.e. the Ids used in getSearchReferences to search for in a filter from multiple sources)
			final Map<String, List<AbstractSelectFilter.SourceSearchTask>> suppliers =
					allSelectFilters.map(f -> f.collectSourceSearchTasks(parser, storage))
									.flatMap(List::stream)
									.collect(Collectors.groupingBy(AbstractSelectFilter.SourceSearchTask::getTargetId));

			final ExecutorService service = Executors.newCachedThreadPool();

			log.debug("Found {} search suppliers", suppliers.size());


			for (Map.Entry<String, List<AbstractSelectFilter.SourceSearchTask>> entry : suppliers.entrySet()) {

				service.submit(() -> {
					final String id = entry.getKey();
					final long begin = System.currentTimeMillis();

					log.info("BEGIN collecting entries for `{}`", id);

					try {
						final List<AbstractSelectFilter.SourceSearchTask> fillers = entry.getValue();
						// Sources can be referenced multiple times (e.g. columns used in many concepts), we want to process them just once.
						// We use seenSources to deduplicate the sources.
						// The duplication can occur because columns might either use a shared-dictionary or a secondaryId which we use as search references.
						final Set<String> seenSources = new HashSet<>(fillers.size());

						final TrieSearch<FEValue> search = new TrieSearch<>();


						fillers.stream()
							   .filter(task -> seenSources.add(task.getSourceId()))
							   .flatMap(AbstractSelectFilter.SourceSearchTask::values)
							   .distinct()
							   .forEach(item -> search.addItem(item, item.extractKeywords()));

						searchCache.put(id, search);

						log.trace("Stats for `{}`", id);

						search.shrinkToFit();

						final long end = System.currentTimeMillis();

						log.debug("DONE collecting entries for `{}`, within {} ({} Items)", id, Duration.ofMillis(end - begin), search.calculateSize());
					}
					catch (Exception e) {
						log.error("Failed to create search for {}", id, e);
					}
				});
			}

			service.shutdown();

			//TODO await properly
			service.awaitTermination(10, TimeUnit.HOURS);


			log.debug("DONE loading SourceSearch");
		}));


	}
}
