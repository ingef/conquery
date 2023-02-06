package com.bakdata.conquery.models.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.validation.Validator;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link com.bakdata.conquery.commands.ShardNode} container of {@link Worker}.
 *
 * Each Shard contains one {@link Worker} per {@link Dataset}.
 */
@Slf4j
public class Workers extends IdResolveContext {
	@Getter @Setter
	private AtomicInteger nextWorker = new AtomicInteger(0);
	@Getter
	private ConcurrentHashMap<WorkerId, Worker> workers = new ConcurrentHashMap<>();
	@JsonIgnore
	private transient Map<DatasetId, Worker> dataset2Worker = new HashMap<>();

	/**
	 * Shared ExecutorService among Workers for Jobs.
	 */
	private final ThreadPoolExecutor jobsThreadPool;
	private final ThreadPoolDefinition queryThreadPoolDefinition;

	private final Supplier<ObjectMapper> persistenceMapperSupplier;
	private final Supplier<ObjectMapper> communicationMapperSupplier;

	private final int entityBucketSize;

	
	public Workers(ThreadPoolDefinition queryThreadPoolDefinition, Supplier<ObjectMapper> persistenceMapperSupplier, Supplier<ObjectMapper> communicationMapperSupplier, int entityBucketSize) {
		this.queryThreadPoolDefinition = queryThreadPoolDefinition;

		jobsThreadPool = queryThreadPoolDefinition.createService("Workers");

		this.persistenceMapperSupplier = persistenceMapperSupplier;
		this.communicationMapperSupplier = communicationMapperSupplier;
		this.entityBucketSize = entityBucketSize;

		jobsThreadPool.prestartAllCoreThreads();
	}

	public Worker createWorker(WorkerStorage storage, boolean failOnError) {

		ObjectMapper persistenceMapper = persistenceMapperSupplier.get();
		this.injectInto(persistenceMapper);

		ObjectMapper communicationMapper = communicationMapperSupplier.get();
		this.injectInto(communicationMapper);

		final Worker worker =
				new Worker(queryThreadPoolDefinition, storage, jobsThreadPool, failOnError, entityBucketSize, persistenceMapper, communicationMapper);

		addWorker(worker);

		return worker;
	}

	public Worker createWorker(Dataset dataset, StoreFactory storageConfig, @NonNull String name, Validator validator, boolean failOnError) {

		ObjectMapper persistenceMapper = persistenceMapperSupplier.get();
		this.injectInto(persistenceMapper);

		ObjectMapper communicationMapper = communicationMapperSupplier.get();
		this.injectInto(communicationMapper);

		final Worker
				worker =
				Worker.newWorker(dataset, queryThreadPoolDefinition, jobsThreadPool, storageConfig, name, validator, failOnError, entityBucketSize, persistenceMapper, communicationMapper);

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
		return null; // Workers simply have no MetaRegistry.
	}

	public void removeWorkerFor(DatasetId dataset) {
		final Worker worker = dataset2Worker.get(dataset);

		/*
		 Close the job manager first, so all jobs are done and none can be added, when the worker is
		 removed from dataset2Worker (which is used in deserialization of NamespacedIds, i.e. content of ForwardToWorkerMessages)
		 */
		worker.getJobManager().close();

		Worker removed = dataset2Worker.remove(dataset);
		if (removed == null) {
			return;
		}

		workers.remove(removed.getInfo().getId());
		try {
			removed.remove();
		}
		catch(Exception e) {
			log.error("Failed to remove storage "+removed, e);
		}
	}
	
	public boolean isBusy() {
		for( Worker worker : workers.values()) {
			if(worker.isBusy()) {
				return true;
			}
		}
		return false;
	}

	public void stop() {
		jobsThreadPool.shutdown();
		for (Worker w : workers.values()) {
			w.close();
		}
	}
}
