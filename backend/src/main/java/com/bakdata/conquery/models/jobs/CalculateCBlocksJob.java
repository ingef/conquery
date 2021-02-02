package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketEntry;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
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
		infos.add(new CalculationInformation(bucket, cBlockId));
	}

	@Override
	public void execute() throws Exception {
		getProgressReporter().setMax(infos.size());

		// todo compute in parallel.
		for (CalculationInformation info : infos) {
			try {
				if (bucketManager.hasCBlock(info.getCBlockId())) {
					log.trace("Skipping calculation of CBlock[{}] because its already present in the BucketManager.");
					continue;
				}

				CBlock cBlock = createCBlock(connector, info);
				cBlock.initIndizes(info.getBucket().getBucketSize());

				connector.calculateCBlock(cBlock, info.getBucket());

				calculateEntityDateIndices(cBlock, info.getBucket());
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
				getProgressReporter().report(1);
			}
		}
		getProgressReporter().done();
	}

	private static CBlock createCBlock(Connector connector, CalculationInformation info) {
		return new CBlock(info.getBucket().getId(), connector.getId());
	}

	/**
	 * For every included entity, calculate min and max and store them as statistics in the CBlock.
	 */
	private void calculateEntityDateIndices(CBlock cBlock, Bucket bucket) {
		Table table = storage.getTable(bucket.getImp().getTable());
		for (Column column : table.getColumns()) {
			if (!column.getType().isDateCompatible()) {
				continue;
			}

			for (BucketEntry entry : bucket.entries()) {
				if (!bucket.has(entry.getEvent(), column)) {
					continue;
				}

				CDateRange range = bucket.getAsDateRange(entry.getEvent(), column);

				if (range.hasLowerBound()) {
					int min = Math.min(
							cBlock.getMinDate().getOrDefault(entry.getEntity(), Integer.MAX_VALUE),
							range.getMinValue()
					);

					cBlock.getMinDate()
						  .put(entry.getEntity(), min);
				}

				if (range.hasUpperBound()) {
					int max = Math.max(
							cBlock.getMaxDate().getOrDefault(entry.getEntity(), Integer.MIN_VALUE),
							range.getMaxValue()
					);

					cBlock.getMaxDate()
						  .put(entry.getEntity(), max);
				}
			}
		}
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	private static class CalculationInformation {

		private final Bucket bucket;
		private final CBlockId cBlockId;
	}
}
