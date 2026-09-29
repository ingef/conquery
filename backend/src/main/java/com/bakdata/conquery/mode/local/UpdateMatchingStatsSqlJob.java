package com.bakdata.conquery.mode.local;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateMatchingStatsSqlJob extends Job {

	@ToString.Exclude
	private final List<Concept<?>> concepts;
	private final Dataset dataset;

	@ToString.Exclude
	private final SqlMatchingStats matchingStats;

	private static void shutdownExecutor(ExecutorService executorService) throws InterruptedException {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
				if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
					log.warn("Matching stats executor did not terminate");
				}
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			throw e;
		}
	}

	@Override
	public void execute() throws Exception {

		log.info("BEGIN collecting {} SQL matching stats for {}", concepts.size(), dataset);

		Stopwatch stopwatch = Stopwatch.createStarted();

		final int nThreads = getMatchingStats().getMatchingStatsWorkers();

		ThreadPoolExecutor executorService = new ThreadPoolExecutor(nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(concepts.size()),
				new ThreadFactoryBuilder().setNameFormat("SQL-MatchingStats-" + getDataset().getName() + "-%d").build()
		);

		try {
			ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
			Map<Future<Void>, TreeConcept> submitted = new HashMap<>();

			for (Concept<?> concept : concepts) {
				if (concept instanceof TreeConcept treeConcept) {
					Future<Void> future = completionService.submit(() -> {
						matchingStats.collectMatchingStatsForConcept(treeConcept, matchingStats.getMatchingStatsRetries());
						return null;
					});

					submitted.put(future, treeConcept);
				}
			}

			while (!submitted.isEmpty()) {
				if (isCancelled()) {
					for (Future<Void> job : submitted.keySet()) {
						job.cancel(true);
					}
					break;
				}

				Future<Void> completedJob = completionService.poll(30, TimeUnit.SECONDS);
				if (completedJob == null) {
					log.debug("WAITING for {} matching stats to finish.", submitted.size());
					continue;
				}

				Concept<?> concept = submitted.remove(completedJob);

				try {
					completedJob.get();
				} catch (ExecutionException e) {
					log.warn("FAILED to collect SQL matching stats for {}", concept, e.getCause());
				} catch (CancellationException e) {
					log.debug("Cancelled SQL matching stats for {}", concept);
				}
			}
		} finally {
			shutdownExecutor(executorService);
		}

		log.debug("DONE collecting SQL matching stats for {} within {}", dataset, stopwatch);
	}

	@Override
	public String getLabel() {
		return "Collect matching stats for %s (%s concepts)".formatted(dataset.getName(), concepts.size());
	}
}
