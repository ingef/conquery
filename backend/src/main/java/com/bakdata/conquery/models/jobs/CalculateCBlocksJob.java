package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeCache;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeChildPrefixIndex;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformation> infos = new ArrayList<>();
	private final WorkerStorage storage;
	private final BucketManager bucketManager;
	private final Connector connector;
	private final Table table;

	@Override
	public String getLabel() {
		return "Calculate " + infos.size() + " CBlocks for " + connector.getId();
	}

	public void addCBlock(Import imp, Bucket bucket, CBlockId cBlockId) {
		infos.add(new CalculationInformation(table, imp, bucket, cBlockId));
	}

	@Override
	public void execute() throws Exception {
		boolean treeConcept = connector.getConcept() instanceof TreeConcept;
		this.progressReporter.setMax(infos.size());

		for (CalculationInformation info : infos) {
			try {
				if (bucketManager.hasCBlock(info.getCBlockId())) {
					continue;
				}

				CBlock cBlock = createCBlock(connector, info);
				cBlock.initIndizes(info.getBucket().getBucketSize());
				if (treeConcept) {
					calculateCBlock(cBlock, (ConceptTreeConnector) connector, info);
				}
				else {
					calculateCBlock(cBlock, (VirtualConceptConnector) connector, info);
				}
				setDateRangeIndex(cBlock, info);
				bucketManager.addCalculatedCBlock(cBlock);
				storage.addCBlock(cBlock);
			}
			catch (Exception e) {
				throw new Exception(
						String.format(
								"Exception in CalculateCBlocksJob (CBlock=%s, connector=%s, table=%s)",
								info.getCBlockId(),
								connector,
								table
						),
						e
				);
			}
			finally {
				this.progressReporter.report(1);
			}
		}
		progressReporter.done();
	}

	private void setDateRangeIndex(CBlock cBlock, CalculationInformation info) {
		Bucket bucket = info.getBucket();
		Table table = storage.getDataset().getTables().get(bucket.getImp().getTable());
		for (Column column : table.getColumns()) {
			if (column.getType().isDateCompatible()) {
				for (BucketEntry entry : bucket.entries()) {
					if (bucket.has(entry.getEvent(), column)) {
						CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);

						cBlock.getMinDate().compute(
								entry.getLocalEntity(),
								(id, value) -> Math.min((value == null) ? Integer.MAX_VALUE : value, range.getMinValue()));

						cBlock.getMaxDate().compute(entry.getLocalEntity(),(id, value) -> Math.max((value == null) ? Integer.MIN_VALUE : value, range.getMaxValue()));
					}
				}
			}
		}
	}

	private CBlock createCBlock(Connector connector, CalculationInformation info) {
		return new CBlock(info.getBucket().getId(), connector.getId());
	}

	/**
	 * No CBlocks for VirtualConcepts
	 */
	private void calculateCBlock(CBlock cBlock, VirtualConceptConnector connector, CalculationInformation info) {}

	private void calculateCBlock(CBlock cBlock, ConceptTreeConnector connector, CalculationInformation info) {

		Bucket bucket = info.getBucket();

		CType<?, ?> cType = info.getImp().getColumns()[connector.getColumn().getPosition()].getType();

		if (!(cType instanceof AStringType)) {
			return;
		}
		AStringType<?> stringType = (AStringType<?>) cType;

		final TreeConcept treeConcept = connector.getConcept();

		final ImportId importId = info.getImp().getId();

		// Create index and insert into Tree.
		TreeChildPrefixIndex.putIndexInto(treeConcept);

		treeConcept.initializeIdCache(stringType, importId);

		final int[][] mostSpecificChildren = new int[bucket.getNumberOfEvents()][];
		cBlock.setMostSpecificChildren(Arrays.asList(mostSpecificChildren));

		final ConceptTreeCache cache = treeConcept.getCache(importId);

		for (BucketEntry entry : bucket.entries()) {
			try {
				final int event = entry.getEvent();

				// Events without values are skipped
				// Events can also be filtered, allowing a single table to be used by multiple connectors.
				if (!bucket.has(event, connector.getColumn())) {
					continue;
				}

				int valueIndex = bucket.getString(event, connector.getColumn());
				final String stringValue = stringType.getElement(valueIndex);

				// Lazy evaluation of map to avoid allocations if possible.
				final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(event, info.getImp()));


				if((connector.getCondition() != null && !connector.getCondition().matches(stringValue, rowMap))){
					continue;
				}

				ConceptTreeChild child = cache.findMostSpecificChild(valueIndex, stringValue, rowMap);

				// Add all Concepts and their path to the root to the CBlock
				if (child == null) {
					continue;
				}

				mostSpecificChildren[event] = child.getPrefix();

				ConceptTreeNode<?> it = child;
				while (it != null) {
					cBlock.getIncludedConcepts()
						  .put(entry.getLocalEntity(), cBlock.getIncludedConcepts().getOrDefault(entry.getLocalEntity(), 0) | it.calculateBitMask());

					it = it.getParent();
				}
			}
			catch (ConceptConfigurationException ex) {
				log.error("Failed to resolve event " + bucket + "-" + entry.getEvent() + " against concept " + connector, ex);
			}
		}

		// see #175 metrics candidate
		log
			.trace(
				"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
				cache.getHits(),
				cache.getMisses(),
				(double) cache.getHits() / cache.getMisses(),
				(double) cache.getHits() / (cache.getHits() + cache.getMisses()));
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	private static class CalculationInformation {

		private final Table table;
		private final Import imp;
		private final Bucket bucket;
		private final CBlockId cBlockId;
	}
}
