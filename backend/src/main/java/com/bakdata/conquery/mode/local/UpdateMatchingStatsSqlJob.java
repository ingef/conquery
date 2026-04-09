package com.bakdata.conquery.mode.local;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.google.common.base.Stopwatch;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringExclude;

@Slf4j
@Data
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

		ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

		List<CompletableFuture<?>> jobs = new ArrayList<>();

		for (Concept<?> concept : concepts) {
			if (!(concept instanceof TreeConcept)) {
				continue;
			}
			jobs.add(matchingStats.collectMatchingStatsForConcept((TreeConcept) concept, executorService).toCompletableFuture());
		}

		CompletableFuture<Void> all = CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new));
		while (!all.isDone()) {
			if (isCancelled()) {
				all.cancel(true);
				log.debug("CANCELLED update matching stats for {}", getDataset(), all.exceptionNow());
				return;
			}

			all.get(5, TimeUnit.SECONDS);
			log.trace("WAITING for matching stats to finish {}", getDataset());

			if (all.isCompletedExceptionally()) {
				log.error("FAILED update matching stats for {}", getDataset(), all.exceptionNow());
				return;
			}
		}

		log.debug("DONE collecting SQL matching stats for {} within {}", dataset, stopwatch);
	}

	@Override
	public String getLabel() {
		return "Collect matching stats for %s (%s concepts)".formatted(dataset.getName(), concepts.size());
	}
}
