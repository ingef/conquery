package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.util.RoundRobinQueue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Workers extends NamespaceCollection implements Closeable {

	private final ThreadPoolExecutor queryThreadPool;

	@Getter
	private final RoundRobinQueue<Runnable> queryExecutorQueues;

	public Workers(RoundRobinQueue<Runnable> queue, int threadPoolSize) {
		super();
		Objects.requireNonNull(queue, "Queues may not be empty.");
		queryExecutorQueues = queue;

		queryThreadPool = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
												 0L, TimeUnit.MILLISECONDS,
												 queryExecutorQueues,
												 new ThreadFactoryBuilder().setNameFormat("QueryExecutor %d").build()
		);
		queryThreadPool.prestartAllCoreThreads();
	}

	@Getter @Setter
	private AtomicInteger nextWorker = new AtomicInteger(0);
	@Getter
	private ConcurrentHashMap<WorkerId, Worker> workers = new ConcurrentHashMap<>();
	@JsonIgnore
	private transient Map<DatasetId, Worker> dataset2Worker = new HashMap<>();

	public void add(Worker worker) {
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
			if(!getQueryExecutorQueues().removeQueue(removed.getQueryExecutor().getJobs())){
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
		// TODO: 26.06.2020 this is probably not enough.
		while (queryThreadPool.awaitTermination(5, TimeUnit.MINUTES)){
			log.info("Waiting for QueryThreadPool to shut down");
		}
	}
}
