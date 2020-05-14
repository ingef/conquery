package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateElementMatchingStats;
import com.bakdata.conquery.models.worker.Worker;
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

		progressReporter.setMax(worker.getStorage().getAllCBlocks().size());

		Map<ConceptElementId<?>, MatchingStats.Entry> messages = new HashMap<>();

		for (CBlock cBlock : new ArrayList<>(worker.getStorage().getAllCBlocks())) {

			if(isCancelled()) {
				progressReporter.done();
				return;
			}

			Concept<?> concept = worker.getStorage().getConcept(cBlock.getConnector().getConcept());
			try {
				Bucket bucket = worker.getStorage().getBucket(cBlock.getBucket());
				Table table = worker.getStorage().getDataset().getTables().get(bucket.getImp().getTable());
				
				for (int event = 0; event < bucket.getNumberOfEvents(); event++) {
					if (concept instanceof TreeConcept && cBlock.getMostSpecificChildren() != null) {
						int[] localIds = cBlock.getMostSpecificChildren().get(event);
						if (localIds != null) {
							ConceptTreeNode<?> e = ((TreeConcept) concept).getElementByLocalId(localIds);
	
							while (e != null) {
								messages.computeIfAbsent(e.getId(), (x) -> new MatchingStats.Entry())
									.addEvent(table, bucket, cBlock, event);
								e = e.getParent();
							}
						}
						else {
							messages
								.computeIfAbsent(concept.getId(), (x) -> new MatchingStats.Entry())
								.addEvent(table, bucket, cBlock, event);
						}
					}
					else {
						messages.computeIfAbsent(concept.getId(), (x) -> new MatchingStats.Entry())
							.addEvent(table, bucket, cBlock, event);
					}
				}
			}
			catch (Exception e) {
				log.error("Failed to collect the matching stats for {}", cBlock, e);
			}

			progressReporter.report(1);
		}

		if (!messages.isEmpty()) {
			worker.send(new UpdateElementMatchingStats(worker.getInfo().getId(), messages));
		}

		progressReporter.done();
	}

	@Override
	public String getLabel() {
		return "updating matching stats for "+worker.getInfo().getId();
	}

}
