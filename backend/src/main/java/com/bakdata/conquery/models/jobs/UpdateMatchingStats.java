package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateElementMatchingStats;
import com.bakdata.conquery.models.worker.Worker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@RequiredArgsConstructor
public class UpdateMatchingStats extends Job {

	@ToString.Exclude
	private final Worker worker;

	@Override
	public void execute() throws Exception {
		if (worker.getStorage().getAllCBlocks().isEmpty()) {
			log.debug("Worker {} is empty, skipping.", worker);
			progressReporter.done();
			return;
		}

		log.debug("Starting to update Matching stats with {}", worker);

		progressReporter.setMax(worker.getStorage().getAllConcepts().size());


		ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(ConqueryConfig.getInstance().getStorage().getThreads())); // TODO refactor this.

		List<ListenableFuture<Map<ConceptElementId<?>, MatchingStats.Entry>>> conceptMatches = new ArrayList<>();

		for (Concept<?> concept :worker.getStorage().getAllConcepts()) {
			conceptMatches.add(pool.submit(() -> calculateConceptMatches(concept)));
		}

		pool.shutdown();

		Map<ConceptElementId<?>, MatchingStats.Entry> messages = new HashMap<>();

		final ListenableFuture<List<Map<ConceptElementId<?>, MatchingStats.Entry>>> chunks = Futures.allAsList(conceptMatches);

		while (true){
			if(isCancelled()){
				pool.shutdownNow();
				return;
			}

			try {
				chunks.get(1, TimeUnit.MINUTES)
					  .forEach(messages::putAll);
				break;
			}catch (TimeoutException ignored){
				// Intentionally left blank
			}
		}

		if (!messages.isEmpty()) {
			worker.send(new UpdateElementMatchingStats(worker.getInfo().getId(), messages));
		}

		progressReporter.done();
	}

	public Map<ConceptElementId<?>, MatchingStats.Entry> calculateConceptMatches(Concept<?> concept) {

		Map<ConceptElementId<?>, MatchingStats.Entry> messages = new HashMap<>();

		for (CBlock cBlock : new ArrayList<>(worker.getStorage().getAllCBlocks())) {

			if(isCancelled())
				return null;

			if(!cBlock.getConnector().getConcept().equals(concept.getId()))
				continue;

			try {
				Bucket bucket = worker.getStorage().getBucket(cBlock.getBucket());
				Table table = worker.getStorage().getDataset().getTables().get(bucket.getImp().getTable());

				for (int event = 0; event < bucket.getNumberOfEvents(); event++) {
					if (!(concept instanceof TreeConcept) || cBlock.getMostSpecificChildren() == null || cBlock.getMostSpecificChildren().get(event) == null) {
						messages.computeIfAbsent(concept.getId(), (x) -> new MatchingStats.Entry())
								.addEvent(table, bucket, cBlock, event);
						continue;
					}

					int[] localIds = cBlock.getMostSpecificChildren().get(event);

					ConceptTreeNode<?> e = ((TreeConcept) concept).getElementByLocalId(localIds);

					while (e != null) {
						messages.computeIfAbsent(e.getId(), (x) -> new MatchingStats.Entry())
								.addEvent(table, bucket, cBlock, event);
						e = e.getParent();
					}
				}
			}
			catch (Exception e) {
				log.error("Failed to collect the matching stats for CBlock " + cBlock.getId(), e);
			}
		}

		progressReporter.report(1);

		return messages;
	}

	@Override
	public String getLabel() {
		return "updating matching stats for "+worker.getInfo().getId();
	}

}
