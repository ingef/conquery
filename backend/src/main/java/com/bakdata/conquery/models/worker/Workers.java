package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.util.RoundRobinQueue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Workers extends NamespaceCollection implements Closeable {


	/**
	 * ThreadPool dedicated to Queries.
	 */
	private final ThreadPoolExecutor queryThreadPool;
	private final RoundRobinQueue<Runnable> queryExecutorQueues;

	@Getter
	private final ThreadPoolExecutor jobsThreadPool;

	public Workers(@NonNull RoundRobinQueue<Runnable> queues, int queryThreadPoolSize, int jobThreadPoolSize) {
		super();
		queryExecutorQueues = queues;

		queryThreadPool = new ThreadPoolExecutor(queryThreadPoolSize, queryThreadPoolSize,
												 0L, TimeUnit.MILLISECONDS,
												 queryExecutorQueues,
												 new ThreadFactoryBuilder().setNameFormat("QueryExecutor %d").build()
		);
		queryThreadPool.prestartAllCoreThreads();

		// TODO: 30.06.2020 build from configuration
		jobsThreadPool = new ThreadPoolExecutor(jobThreadPoolSize / 2, jobThreadPoolSize,
												60L, TimeUnit.SECONDS,
												new LinkedBlockingQueue<>(),
												new ThreadFactoryBuilder().setNameFormat("Workers Helper %d").build()
		);

		jobsThreadPool.prestartAllCoreThreads();
	}

	@Getter @Setter
	private AtomicInteger nextWorker = new AtomicInteger(0);
	@Getter
	private ConcurrentHashMap<WorkerId, Worker> workers = new ConcurrentHashMap<>();
	@JsonIgnore
	private transient Map<DatasetId, Worker> dataset2Worker = new HashMap<>();


	public Worker createWorker(WorkerInformation info, WorkerStorage storage) {
		final JobManager jobManager = new JobManager(info.getName());
		final BucketManager bucketManager = new BucketManager(jobManager, storage, info);

		storage.setBucketManager(bucketManager);


		final QueryExecutor queryExecutor = new QueryExecutor(queryExecutorQueues.createQueue());

		final Worker worker = new Worker(info, jobManager, storage, queryExecutor, jobsThreadPool);
		addWorker(worker);

		return worker;
	}

	private void addWorker(Worker worker) {
		nextWorker.incrementAndGet();
		workers.put(worker.getInfo().getId(), worker);
		dataset2Worker.put(worker.getStorage().getDataset().getId(), worker);
	}

	public Worker getWorker(WorkerId worker) {
		return Objects.requireNonNull(workers.get(worker));
	}

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) {
		if (!dataset2Worker.containsKey(dataset)) {
			throw new NoSuchElementException(String.format("Did not find Dataset[%s] in [%s]", dataset, dataset2Worker.keySet()));
		}

		return dataset2Worker.get(dataset).getStorage().getCentralRegistry();
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		throw new UnsupportedOperationException("Workers should never be asked about the meta registry");
	}

	public void removeWorkersFor(DatasetId dataset) {
		Worker removed = dataset2Worker.remove(dataset);
		if(removed == null) {
			return;
		}

		workers.remove(removed.getInfo().getId());
		try {
			removed.getJobManager().stop();
			removed.getStorage().remove();
			if(!queryExecutorQueues.removeQueue(removed.getQueryExecutor().getJobs())){
				log.warn("Queue for Worker[{}] did not exist.", removed.getInfo().getDataset());
			}
		}
		catch(Exception e) {
			log.error("Failed to remove storage "+removed, e);
		}
	}

	@SneakyThrows(InterruptedException.class)
	@Override
	public void close() throws IOException {
		queryThreadPool.shutdown();
		jobsThreadPool.shutdown();

		while (queryThreadPool.awaitTermination(2, TimeUnit.MINUTES) || jobsThreadPool.awaitTermination(2, TimeUnit.MINUTES)){
			log.debug("Waiting for ThreadPools to shut down");
		}
	}

	public boolean isBusy() {
		for( Worker worker : workers.values()) {
			if(worker.isBusy()) {
				return true;
			}
		}

		return  jobsThreadPool.getActiveCount() != 0 || !jobsThreadPool.getQueue().isEmpty()
			   || queryThreadPool.getActiveCount() != 0 || !queryExecutorQueues.isEmpty();
	}


}
