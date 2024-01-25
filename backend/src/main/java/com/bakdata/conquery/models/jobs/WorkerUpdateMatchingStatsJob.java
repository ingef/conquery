package com.bakdata.conquery.models.jobs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.mode.cluster.WorkerMatchingStats;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateElementMatchingStats;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;
import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WorkerUpdateMatchingStatsJob extends Job {

	private final Worker worker;
	private final Collection<Concept<?>> concepts;

	@Override
	public String getLabel() {
		return String.format("Calculate Matching Stats for %s", worker.getInfo().getDataset());
	}

	@Override
	public void execute() throws Exception {

		if (worker.getStorage().getAllCBlocks().isEmpty()) {
			log.debug("Worker {} is empty, skipping.", worker);
			return;
		}

		final ProgressReporter progressReporter = getProgressReporter();
		progressReporter.setMax(concepts.size());

		log.info("BEGIN update Matching stats for {} Concepts", concepts.size());

		final Map<? extends Concept<?>, CompletableFuture<Void>>
				subJobs =
				concepts.stream()
						.collect(Collectors.toMap(
								Functions.identity(),
								concept -> CompletableFuture.runAsync(() -> {
									final Map<ConceptElement<?>, WorkerMatchingStats.Entry>
											matchingStats =
											new HashMap<>(concept.countElements());

									calculateConceptMatches(concept, matchingStats, worker);

									worker.send(new UpdateElementMatchingStats(worker.getInfo().getId(), matchingStats));

									progressReporter.report(1);
								}, worker.getJobsExecutorService())
						));


		log.debug("All jobs submitted. Waiting for completion.");


		final CompletableFuture<Void> all = CompletableFuture.allOf(subJobs.values().toArray(CompletableFuture[]::new));

		do {
			try {
				all.get(1, TimeUnit.MINUTES);
			}
			catch (TimeoutException exception) {
				// Count unfinished matching stats jobs.
				if (log.isDebugEnabled()) {
					final long unfinished = subJobs.values().stream().filter(Predicate.not(CompletableFuture::isDone)).count();
					log.debug("{} still waiting for {} tasks", worker.getInfo().getDataset(), unfinished);
				}

				// When trace, also log the unfinished jobs.
				if (log.isTraceEnabled()) {
					subJobs.forEach((concept, future) -> {
						if (future.isDone()) {
							return;
						}

						log.trace("Still waiting for `{}`", concept.getId());

					});
				}
			}
		} while (!all.isDone());

		log.debug("DONE collecting matching stats for {}", worker.getInfo().getDataset());

	}

	private static void calculateConceptMatches(Concept<?> concept, Map<ConceptElement<?>, WorkerMatchingStats.Entry> results, Worker worker) {
		log.debug("BEGIN calculating for `{}`", concept.getId());

		for (CBlock cBlock : worker.getStorage().getAllCBlocks()) {

			if (!cBlock.getConnector().getConcept().equals(concept)) {
				continue;
			}

			try {
				final Bucket bucket = cBlock.getBucket();
				final Table table = bucket.getTable();

				for (int entity : bucket.entities()) {

					final int entityEnd = bucket.getEntityEnd(entity);

					for (int event = bucket.getEntityStart(entity); event < entityEnd; event++) {

						final int[] localIds = cBlock.getPathToMostSpecificChild(event);


						if (!(concept instanceof TreeConcept) || localIds == null) {

							results.computeIfAbsent(concept, (ignored) -> new WorkerMatchingStats.Entry()).addEvent(table, bucket, event, entity);

							continue;
						}

						if (Connector.isNotContained(localIds)) {
							continue;
						}

						ConceptTreeNode<?> element = ((TreeConcept) concept).getElementByLocalIdPath(localIds);

						while (element != null) {
							results.computeIfAbsent(((ConceptElement<?>) element), (ignored) -> new WorkerMatchingStats.Entry())
								   .addEvent(table, bucket, event, entity);
							element = element.getParent();
						}
					}
				}

			}
			catch (Exception e) {
				log.error("Failed to collect the matching stats for {}", cBlock, e);
			}
		}

		log.trace("DONE calculating for `{}`", concept.getId());
	}

}
