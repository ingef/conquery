package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateElementMatchingStats;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.util.progressreporter.ProgressReporter;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString @RequiredArgsConstructor
public class UpdateMatchingStats extends Job {

	@ToString.Exclude
	private final Workers workers;

	@Override
	public void execute() throws Exception {
		if(workers.getWorkers().isEmpty())
			return;
		progressReporter.setMax(workers.getWorkers().values().size());
		
		for(Worker w:new ArrayList<>(workers.getWorkers().values())) {
			ProgressReporter sub = progressReporter.subJob(1);
			if(!w.getStorage().getAllCBlocks().isEmpty()) {
				sub.setMax(w.getStorage().getAllCBlocks().size());
				
				Map<ConceptElementId<?>, MatchingStats.Entry> messages = new HashMap<>();
				
				for(CBlock cBlock : new ArrayList<>(w.getStorage().getAllCBlocks())) {
					Concept<?> concept = w.getStorage().getConcept(cBlock.getConnector().getConcept());
					Block block = w.getStorage().getBlock(cBlock.getBlock());
					Table table = w.getStorage().getDataset().getTables().get(block.getId().getImp().getTable());
					
					try {
						for(int event=0;event<block.size();event++) {
							if(concept instanceof TreeConcept) {
								int[] localIds = cBlock.getMostSpecificChildren().get(event);
								if(localIds != null) {
									ConceptTreeNode<?> e = ((TreeConcept) concept).getElementByLocalId(localIds);
									
									while(e != null) {
										messages
											.computeIfAbsent(e.getId(), (x)->new MatchingStats.Entry())
											.addEvent(table, block, cBlock, event);
										e = e.getParent();
									}
								}
							}
							else {
								messages
									.computeIfAbsent(concept.getId(), (x)->new MatchingStats.Entry())
									.addEvent(table, block, cBlock, event);
							}
						}
					}
					catch(Exception e) {
						log.error("Failed to collect the matching stats for CBlock "+cBlock.getId(), e);
					}
					
					sub.report(1);
				}
				if(!messages.isEmpty()) {
					w.send(new UpdateElementMatchingStats(w.getInfo().getId(), messages));
				}
			}
			sub.done();
		}
		progressReporter.done();
	}

	@Override
	public String getLabel() {
		return toString();
	}

}
