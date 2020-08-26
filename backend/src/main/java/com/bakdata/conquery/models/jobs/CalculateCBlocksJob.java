package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
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

/**
 * Calculate CBlocks, ie the Connection between a Concept and a Bucket.
 * <p>
 * If a Bucket x Connector has a CBlock, the ConceptNode will rely on that to iterate events. If not, it will fall back onto equality checks.
 */
@RequiredArgsConstructor
@Slf4j
public class CalculateCBlocksJob extends Job {

	private static final int[] NOT_CONTAINED = new int[]{-1};

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
		infos.add(new CalculationInformation(imp, bucket, cBlockId));
	}

	@Override
	public void execute() throws Exception {
		this.progressReporter.setMax(infos.size());

		for (CalculationInformation info : infos) {
			try {
				if (bucketManager.hasCBlock(info.getCBlockId())) {
					continue;
				}

				CBlock cBlock = createCBlock(connector, info);
				cBlock.initIndizes(info.getBucket().getBucketSize());
				if (connector.getConcept() instanceof TreeConcept) {
					final ConceptTreeConnector treeConnector = (ConceptTreeConnector) connector;
					calculateCBlock(cBlock, treeConnector, info);
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
			if (!column.getType().isDateCompatible()) {
				continue;
			}

			for (BucketEntry entry : bucket.entries()) {
				if (!bucket.has(entry.getEvent(), column)) {
					continue;
				}

				CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);

				cBlock.getMinDate()[entry.getLocalEntity()] = Math.min(cBlock.getMinDate()[entry.getLocalEntity()], range.getMinValue());

				cBlock.getMaxDate()[entry.getLocalEntity()] = Math.max(cBlock.getMaxDate()[entry.getLocalEntity()], range.getMaxValue());
			}
		}
	}

	private CBlock createCBlock(Connector connector, CalculationInformation info) {
		return new CBlock(info.getBucket().getId(), connector.getId());
	}

	/**
	 * No CBlocks for VirtualConcepts
	 */
	private void calculateCBlock(CBlock cBlock, VirtualConceptConnector connector, CalculationInformation info) {
	}


	private void calculateCBlock(CBlock cBlock, ConceptTreeConnector connector, CalculationInformation info) {

		Bucket bucket = info.getBucket();

		final Column column = connector.getColumn();


		final TreeConcept treeConcept = connector.getConcept();

		final ImportId importId = info.getImp().getId();

		final AStringType<?> stringType;


		if (column != null) {

			CType<?, ?> cType = info.getImp().getColumns()[column.getPosition()].getType();

			if (!(cType instanceof AStringType)) {
				log.warn("Column[{}] is not of String type, but `{}`", column.getId(), cType);
				return;
			}
			stringType = (AStringType<?>) cType;

			// Create index and insert into Tree.
			TreeChildPrefixIndex.putIndexInto(treeConcept);

			treeConcept.initializeIdCache(stringType, importId);
		}
		else {
			stringType = null;
		}


		final List<int[]> mostSpecificChildren = new ArrayList<>(bucket.getNumberOfEvents());

		final ConceptTreeCache cache = treeConcept.getCache(importId);

		final int[] root = connector.getConcept().getPrefix();

		for (BucketEntry entry : bucket.entries()) {
			try {
				final int event = entry.getEvent();

				// Events without values are omitted
				// Events can also be filtered, allowing a single table to be used by multiple connectors.
				if (column != null && !bucket.has(event, column)) {
					mostSpecificChildren.add(NOT_CONTAINED);
					continue;
				}
				String stringValue = "";
				int valueIndex = -1;

				if (stringType != null) {
					valueIndex = bucket.getString(event, column);
					stringValue = stringType.getElement(valueIndex);
				}

				// Lazy evaluation of map to avoid allocations if possible.
				final CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(() -> bucket.calculateMap(event, info.getImp()));


				if ((connector.getCondition() != null && !connector.getCondition().matches(stringValue, rowMap))) {
					mostSpecificChildren.add(NOT_CONTAINED);
					continue;
				}

				ConceptTreeChild child = cache == null
										 ? treeConcept.findMostSpecificChild(stringValue, rowMap)
										 : cache.findMostSpecificChild(valueIndex, stringValue, rowMap);

				// Add all Concepts and their path to the root to the CBlock
				if (child == null) {
					// see #174 improve handling by copying the relevant things from the old project
					mostSpecificChildren.add(root);
					continue;
				}

				mostSpecificChildren.add(child.getPrefix());
				ConceptTreeNode<?> it = child;
				while (it != null) {
					cBlock.getIncludedConcepts()[entry.getLocalEntity()] |= it.calculateBitMask();
					it = it.getParent();
				}
			}
			catch (ConceptConfigurationException ex) {
				log.error("Failed to resolve event " + bucket + "-" + entry.getEvent() + " against concept " + connector, ex);
			}
		}

		cBlock.setMostSpecificChildren(mostSpecificChildren);


		if (cache != null) {
			log.trace(
					"Hits: {}, Misses: {}, Hits/Misses: {}, %Hits: {} (Up to now)",
					cache.getHits(),
					cache.getMisses(),
					(double) cache.getHits() / cache.getMisses(),
					(double) cache.getHits() / (cache.getHits() + cache.getMisses())
			);
		}
	}


	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	private static class CalculationInformation {

		private final Import imp;
		private final Bucket bucket;
		private final CBlockId cBlockId;
	}
}
