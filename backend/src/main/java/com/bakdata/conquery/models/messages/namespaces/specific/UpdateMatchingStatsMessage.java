package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.base.Functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * For each {@link com.bakdata.conquery.models.query.queryplan.specific.ConceptNode} calculate the number of matching events and the span of date-ranges.
 */
@CPSType(id = "UPDATE_MATCHING_STATS", base = NamespacedMessage.class)
@Slf4j
public class UpdateMatchingStatsMessage extends WorkerMessage.Slow {


	@Override
	public void react(Worker worker) throws Exception {
		worker.getJobManager().addSlowJob(new UpdateMatchingStatsJob(worker));
	}

	@RequiredArgsConstructor
	private static class UpdateMatchingStatsJob extends Job {
		private final Worker worker;

		@Override
		public String getLabel() {
			return String.format("Calculate Matching Stats for %s", worker.getInfo().getDataset());
		}

		@Override
		public void execute() throws Exception {
			if (worker.getStorage().getAllCBlocks().isEmpty()) {
				log.debug("Worker {} is empty, skipping.", worker);
				getProgressReporter().done();
				return;
			}

			getProgressReporter().setMax(worker.getStorage().getAllConcepts().size());

			log.info("BEGIN update Matching stats for {} Concepts", worker.getStorage().getAllConcepts().size());

			// SubJobs collect into this Map.
			final ConcurrentMap<ConceptElement<?>, MatchingStats.Entry> messages =
					new ConcurrentHashMap<>(worker.getStorage().getAllConcepts().size()
											* 5_000); // Just a guess-timate so we don't grow that often, this memory is very short lived so we can over commit.


			Map<? extends Concept<?>, CompletableFuture<?>> subJobs =
					worker.getStorage().getAllConcepts()
						  .stream()
						  .collect(Collectors.toMap(
								  Functions.identity(),
								  concept -> CompletableFuture.runAsync(() -> calculateConceptMatches(concept, messages, worker), worker.getJobsExecutorService())
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

			log.debug("All threads are done.");

			if (messages.isEmpty()) {
				log.warn("Results were empty.");
			}
			else {
				worker.send(new UpdateElementMatchingStats(worker.getInfo().getId(), messages));
			}

			getProgressReporter().done();
		}


		private void calculateConceptMatches(Concept<?> concept, Map<ConceptElement<?>, MatchingStats.Entry> results, Worker worker) {
			log.debug("BEGIN calculating for `{}`", concept.getId());

			for (CBlock cBlock : worker.getStorage().getAllCBlocks()) {

				if (!cBlock.getConnector().getConcept().equals(concept)) {
					continue;
				}

				try {
					Bucket bucket = cBlock.getBucket();
					Table table = bucket.getTable();

					for (int entity : bucket.entities()) {

						final int entityEnd = bucket.getEntityEnd(entity);

						for (int event = bucket.getEntityStart(entity); event < entityEnd; event++) {

							final int[] localIds = cBlock.getEventMostSpecificChild(event);


							if (!(concept instanceof TreeConcept) || localIds == null) {

								results.computeIfAbsent(concept, (ignored) -> new MatchingStats.Entry())
									   .addEvent(table, bucket, event, entity);

								continue;
							}

							if (Connector.isNotContained(localIds)) {
								continue;
							}

							ConceptTreeNode<?> element = ((TreeConcept) concept).getElementByLocalId(localIds);

							while (element != null) {
								results.computeIfAbsent(((ConceptElement<?>) element), (ignored) -> new MatchingStats.Entry())
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

			getProgressReporter().report(1);

			log.trace("DONE calculating for `{}`", concept.getId());
		}

	}


}
