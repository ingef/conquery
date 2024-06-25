package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Calculate CBlocks, ie the Connection between a Concept and a Bucket.
 * <p>
 * If a Bucket x Connector has a CBlock, the ConceptNode will rely on that to iterate events. If not, it will fall back onto equality checks.
 */
@RequiredArgsConstructor
@Slf4j
@Data
@ToString(onlyExplicitlyIncluded = true)
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformation> infos = new ArrayList<>();
	private final WorkerStorage storage;
	private final BucketManager bucketManager;
	private final ExecutorService executorService;

	@ToString.Include
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

		log.info("BEGIN calculate CBlocks for {} entries.", infos.size());

		getProgressReporter().setMax(infos.size());


		final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(getExecutorService());

		final List<? extends ListenableFuture<?>> futures =
				infos.stream()
					 .map(this::createInformationProcessor)
					 .map(executorService::submit)
					 .collect(Collectors.toList());

		log.debug("DONE CalculateCBlocks for {} entries.", infos.size());


		final ListenableFuture<?> all = Futures.allAsList(futures);

		while(!all.isDone()) {
			try {
				all.get(1, TimeUnit.MINUTES);
			}
			catch (TimeoutException exception) {
				log.debug("submitted={}, pool={}", infos, getExecutorService());
			}
		}
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

	@Data
	private static class CalculationInformation {
		private final ConceptTreeConnector connector;
		private final Bucket bucket;

		public CBlockId getCBlockId() {
			return new CBlockId(getBucket().getId(), getConnector().getId());
		}
	}


	@Data
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

					log.trace("BEGIN calculating CBlock for {}" , info);

					final CBlock cBlock = CBlock.createCBlock(info.getConnector(), info.getBucket(), bucketManager.getEntityBucketSize());

					log.trace("DONE calculating CBlock for {}" , info);

					bucketManager.addCalculatedCBlock(cBlock);
					storage.addCBlock(cBlock);
				}
			}
			catch (Exception e) {
				throw new RuntimeException("Exception in CalculateCBlocksJob %s".formatted(info), e);
			}
		}

	}
}
