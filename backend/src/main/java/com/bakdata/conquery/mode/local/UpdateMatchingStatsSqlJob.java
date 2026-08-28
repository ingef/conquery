package com.bakdata.conquery.mode.local;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;

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

		ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
			Executors.newFixedThreadPool(getMatchingStats().getMatchingStatsWorkers()));

		Map<ConceptId, ListenableFuture<?>> jobsByConcept = new HashedMap<>();
		Collection<ListenableFuture<?>> jobs = jobsByConcept.values();

		for (Concept<?> concept : concepts) {
			if (concept instanceof TreeConcept) {
				ListenableFuture<?> job = matchingStats.collectMatchingStatsForConcept(
					(TreeConcept) concept,
					executorService,
					getMatchingStats().getMatchingStatsRetries());

				job.addListener(
					() -> {
						if (job.state().equals(Future.State.FAILED)) {
							log.warn("FAILED to collect SQL matching stats for {}", concept, job.exceptionNow());
						}
					},
					MoreExecutors.directExecutor());

				jobsByConcept.put(concept.getId(), job);
			}
		}

		while (jobs.stream().anyMatch(job -> job.state().equals(Future.State.RUNNING))) {
			if (isCancelled()) {
				for (ListenableFuture<?> job : jobs) {
					job.cancel(true);
				}
			}

			for (ListenableFuture<?> someJob : jobs) {
				if (someJob.isDone()) {
					continue;
				}

				try {
					someJob.get(30, TimeUnit.SECONDS);
				} catch (Exception e) {
					// intentionally left blank
				}

				log.debug(
					"WAITING for {} matching stats to finish.",
					jobs.stream().filter(job -> job.state().equals(Future.State.RUNNING)).count());
			}
		}

		log.debug("DONE collecting SQL matching stats for {} within {}", dataset, stopwatch);
	}

	@Override
	public String getLabel() {
		return "Collect matching stats for %s (%s concepts)".formatted(dataset.getName(), concepts.size());
	}
}
