package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.specific.IStringType;
import com.bakdata.conquery.util.CalculatedValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformation> infos = new ArrayList<>();
	private final WorkerStorage storage;
	private final BlockManager blockManager;
	private final Connector connector;
	private final Table table;

	@Override
	public String getLabel() {
		return "Calculate "+infos.size()+" CBlocks for "+connector.getId();
	}
	
	public void addCBlock(Import imp, Block block, CBlockId cBlockId) {
		infos.add(new CalculationInformation(table, imp, block, cBlockId));
	}
	
	@Override
	public void execute() throws Exception {
		boolean treeConcept = connector.getConcept() instanceof TreeConcept;
		this.progressReporter.setMax(infos.size());

		for(int i=0;i<infos.size();i++) {
			try {
				if (!blockManager.hasCBlock(infos.get(i).getCBlockId())) {
					CBlock cBlock = createCBlock(connector, infos.get(i));
					if (treeConcept) {
						calculateCBlock(cBlock, (ConceptTreeConnector) connector, infos.get(i));
					}
					else {
						calculateCBlock(cBlock, (VirtualConceptConnector) connector, infos.get(i));
					}
					blockManager.addCalculatedCBlock(cBlock);
					storage.addCBlock(cBlock);
				}
			}
			catch (Exception e){
				throw new Exception(String.format("Exception in CalculateCBlocksJob (CBlock=%s, connector=%s, table=%s)", infos.get(i).getCBlockId(), connector, table), e);
			}
			finally {
				this.progressReporter.report(1);
			}
		}
	}
	
	private CBlock createCBlock(Connector connector, CalculationInformation info) {
		return new CBlock(
			info.getBlock().getId(),
			connector.getId()
		);
	}

	private void calculateCBlock(CBlock cBlock, VirtualConceptConnector connector, CalculationInformation info) {
	}

	private void calculateCBlock(CBlock cBlock, ConceptTreeConnector connector, CalculationInformation info) {

		IStringType stringType = (IStringType) info.getImp().getColumns()[connector.getColumn().getPosition()].getType();

		final TreeConcept treeConcept = connector.getConcept();

		final ImportId importId = info.getImp().getId();

		TreeChildPrefixIndex.putIndexInto(treeConcept);

		treeConcept.initializeIdCache(stringType, importId);

		cBlock.setMostSpecificChildren(new ArrayList<>(info.getBlock().size()));
		Block block = info.getBlock();

		final ConceptTreeCache cache = treeConcept.getCache(importId);

		for(int event = 0; event < block.size(); event++) {
			try {
				if(block.has(event, connector.getColumn())) {
					int valueIndex = block.getString(event, connector.getColumn());
					final int finalEvent = event;
					final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(
							() -> block.calculateMap(finalEvent, info.getImp())
					);

					ConceptTreeChild child = cache.findMostSpecificChild(valueIndex, rowMap);

					if (child != null) {
						cBlock.getMostSpecificChildren().add(child.getPrefix());
					}
					else {
						//see #174  improve handling by copying the relevant things from the old project
						cBlock.getMostSpecificChildren().add(null);
					}
				}
				else {
					cBlock.getMostSpecificChildren().add(null);
				}
			} catch (ConceptConfigurationException ex) {
				log.error("Failed to resolve event "+block+"-"+event+" against concept "+connector, ex);
			}
		}

		//see #175  metrics candidate
		log.trace(
				"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
				cache.getHits(),
				cache.getMisses(),
				(double) cache.getHits() / cache.getMisses(),
				(double) cache.getHits() / (cache.getHits() + cache.getMisses())
		);
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor @Getter @Setter
	private static class CalculationInformation {
		private final Table table;
		private final Import imp;
		private final Block block;
		private final CBlockId cBlockId;
	}
}
