package com.bakdata.conquery.models.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.Getter;
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
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class CalculateCBlocksJob extends Job {

	private final List<CalculationInformationProcessor> tasks = new ArrayList<>();
	private final WorkerStorage storage;
	private final BucketManager bucketManager;
	private final ExecutorService executorService;

	public void addCBlock(Bucket bucket, ConceptTreeConnector connector) {
		tasks.add(createInformationProcessor(connector, bucket));
	}

	private CalculationInformationProcessor createInformationProcessor(ConceptTreeConnector connector, Bucket bucket) {
		return new CalculationInformationProcessor(connector, bucket, bucketManager, storage);
	}

	@Override
	public void execute() throws Exception {
		if (tasks.isEmpty()) {
			return;
		}

		log.info("BEGIN calculate CBlocks for {} entries.", tasks.size());

		getProgressReporter().setMax(tasks.size());

		final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(getExecutorService());

		final List<? extends ListenableFuture<?>> futures =
				tasks.stream()
					 .map(executorService::submit)
					 .peek(fut -> fut.addListener(this::incrementProgressReporter, MoreExecutors.directExecutor()))
					 .collect(Collectors.toList());


		final ListenableFuture<?> all = Futures.allAsList(futures);

		while (!all.isDone()) {
			try {
				all.get(1, TimeUnit.MINUTES);
			}
			catch (TimeoutException exception) {
				log.debug("submitted={}, pool={}", tasks.size(), getExecutorService());

				if (log.isTraceEnabled() && getExecutorService() instanceof ThreadPoolExecutor) {
					log.trace("Waiting for {}", ((ThreadPoolExecutor) getExecutorService()).getQueue());
				}
			}
		}

		log.debug("DONE CalculateCBlocks for {} entries.", tasks.size());

	}

	@ToString.Include
	@Override
	public String getLabel() {
		return "Calculate CBlocks[" + tasks.size() + "]";
	}

	private void incrementProgressReporter() {
		getProgressReporter().report(1);
	}

	public boolean isEmpty() {
		return tasks.isEmpty();
	}


	@Data
	@ToString(onlyExplicitlyIncluded = true)
	private static class CalculationInformationProcessor implements Runnable {
		private final ConceptTreeConnector connector;
		private final Bucket bucket;

		private final BucketManager bucketManager;
		private final WorkerStorage storage;

		@Override
		public void run() {
			try {
				if (bucketManager.hasCBlock(getCBlockId())) {
					log.trace("Skipping calculation of CBlock[{}] because its already present in the BucketManager.", getCBlockId());
					return;
				}

				log.trace("BEGIN calculating CBlock for {}", getCBlockId());

				final CBlock cBlock = CBlock.createCBlock(getConnector(), getBucket(), bucketManager);

				log.trace("DONE calculating CBlock for {}", getCBlockId());

				bucketManager.addCalculatedCBlock(cBlock);
				storage.addCBlock(cBlock);
			}
			catch (Exception e) {
				throw new RuntimeException("Exception in CalculateCBlocksJob %s".formatted(getCBlockId()), e);
			}
		}

		@ToString.Include
		public CBlockId getCBlockId() {
			return new CBlockId(getBucket().getId(), getConnector().getId());
		}

	}
}
