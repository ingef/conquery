package com.bakdata.conquery.mode.local;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.google.common.base.Stopwatch;
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


	@Override
	public void execute() throws Exception {

		log.info("BEGIN collecting SQL matching stats for {}", dataset);

		Stopwatch stopwatch = Stopwatch.createStarted();

		ExecutorService executorService = Executors.newFixedThreadPool(getMatchingStats().getMatchingStatsWorkers());
		try {
			ExecutorCompletionService<TreeConcept> completionService = new ExecutorCompletionService<>(executorService);
			Map<Future<TreeConcept>, TreeConcept> activeJobs = new HashMap<>(getMatchingStats().getMatchingStatsWorkers());
			Iterator<Concept<?>> remainingConcepts = concepts.iterator();

			for (int worker = 0; worker < getMatchingStats().getMatchingStatsWorkers(); worker++) {
				if (!submitNext(remainingConcepts, completionService, activeJobs)) {
					break;
				}
			}

			while (!activeJobs.isEmpty()) {
				if (isCancelled()) {
					activeJobs.keySet().forEach(job -> job.cancel(true));
					break;
				}

				Future<TreeConcept> completedJob = completionService.poll(30, TimeUnit.SECONDS);
				if (completedJob == null) {
					log.debug("WAITING for {} matching stats to finish.", activeJobs.size());
					continue;
				}

				TreeConcept concept = activeJobs.remove(completedJob);
				try {
					completedJob.get();
				} catch (ExecutionException e) {
					log.warn("FAILED to collect SQL matching stats for {}", concept, e.getCause());
				} catch (CancellationException e) {
					log.debug("Cancelled SQL matching stats for {}", concept);
				}

				submitNext(remainingConcepts, completionService, activeJobs);
			}
		} finally {
			shutdownExecutor(executorService);
		}

		log.debug("DONE collecting SQL matching stats for {} within {}", dataset, stopwatch);
	}

	private boolean submitNext(
			Iterator<Concept<?>> remainingConcepts,
			ExecutorCompletionService<TreeConcept> completionService,
			Map<Future<TreeConcept>, TreeConcept> activeJobs
	) {
		while (remainingConcepts.hasNext()) {
			Concept<?> concept = remainingConcepts.next();
			if (!(concept instanceof TreeConcept treeConcept)) {
				continue;
			}

			Future<TreeConcept> future = completionService.submit(() -> {
				matchingStats.collectMatchingStatsForConcept(treeConcept, matchingStats.getMatchingStatsRetries());
				return treeConcept;
			});
			activeJobs.put(future, treeConcept);
			return true;
		}
		return false;
	}

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
	public String getLabel() {
		return "Collect matching stats for %s (%s concepts)".formatted(dataset.getName(), concepts.size());
	}
}
