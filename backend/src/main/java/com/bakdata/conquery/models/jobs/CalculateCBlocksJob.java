package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
	private final ExecutorService executorService;

	@Override
	public String getLabel() {
		return "Calculate CBlocks[" + infos.size() + "]";
	}

	public void addCBlock(Bucket bucket, ConceptTreeConnector connector) {
		infos.add(new CalculationInformation(connector, bucket));
	}

	@Override
	public void execute() throws Exception {
		if(infos.isEmpty()){
			return;
		}

		getProgressReporter().setMax(infos.size());

		final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(this.executorService);

		final List<? extends ListenableFuture<?>> futures = infos.stream()
				.map(this::createInformationProcessor)
				.map(executorService::submit)
				.peek(f -> f.addListener(this::incrementProgressReporter, MoreExecutors.directExecutor()))
				.collect(Collectors.toList());

		Futures.allAsList(futures).get();

		getProgressReporter().done();
	}

	private CalculationInformationProcessor createInformationProcessor(CalculationInformation info) {
		return new CalculationInformationProcessor(info, bucketManager, storage);
	}

	private void incrementProgressReporter() {
		getProgressReporter().report(1);
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	@RequiredArgsConstructor
	@Getter
	@Setter
	private static class CalculationInformation {
		private final ConceptTreeConnector connector;
		private final Bucket bucket;

		public CBlockId getCBlockId() {
			return new CBlockId(getBucket().getId(), getConnector().getId());
		}
	}


	@RequiredArgsConstructor
	private static class CalculationInformationProcessor implements Runnable {
		private final CalculationInformation info;
		private final BucketManager bucketManager;
		private final WorkerStorage storage;

		@Override
		public void run() {
			try {
				try(IdMutex.Locked ignored = bucketManager.acquireLock(info.connector)) {
					if (bucketManager.hasCBlock(info.getCBlockId())) {
						log.trace("Skipping calculation of CBlock[{}] because its already present in the BucketManager.", info.getCBlockId());
						return;
					}

					CBlock cBlock = CBlock.createCBlock(info.getConnector(), info.getBucket(), bucketManager.getEntityBucketSize());

					bucketManager.addCalculatedCBlock(cBlock);
					storage.addCBlock(cBlock);
				}
			}
			catch (Exception e) {
				throw new RuntimeException(
						String.format(
								"Exception in CalculateCBlocksJob (CBlock=%s, connector=%s)",
								info.getCBlockId(),
								info.getConnector()
						),
						e
				);
			}
		}

	}
}
