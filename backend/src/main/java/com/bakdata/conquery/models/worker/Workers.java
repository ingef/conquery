package com.bakdata.conquery.models.worker;

import java.util.Collection;
import java.util.Collections;
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
	private final ConcurrentHashMap<DatasetId, Worker> workers = new ConcurrentHashMap<>();

	/**
	 * Shared ExecutorService among Workers for Jobs.
	 */
	private final ThreadPoolExecutor jobsThreadPool;
	private final ThreadPoolDefinition queryThreadPoolDefinition;

	private final Supplier<ObjectMapper> persistenceMapperSupplier;
	private final Supplier<ObjectMapper> communicationMapperSupplier;

	private final int entityBucketSize;

	private final int secondaryIdSubPlanRetention;

	
	public Workers(ThreadPoolDefinition queryThreadPoolDefinition, Supplier<ObjectMapper> persistenceMapperSupplier, Supplier<ObjectMapper> communicationMapperSupplier, int entityBucketSize, int secondaryIdSubPlanRetention) {
		this.queryThreadPoolDefinition = queryThreadPoolDefinition;

		// TODO This shouldn't be coupled to the query thread pool definition
		jobsThreadPool = queryThreadPoolDefinition.createService("Workers");

		this.persistenceMapperSupplier = persistenceMapperSupplier;
		this.communicationMapperSupplier = communicationMapperSupplier;
		this.entityBucketSize = entityBucketSize;
		this.secondaryIdSubPlanRetention = secondaryIdSubPlanRetention;

		jobsThreadPool.prestartAllCoreThreads();
	}

	public Worker createWorker(WorkerStorage storage, boolean failOnError) {

		final ObjectMapper persistenceMapper = persistenceMapperSupplier.get();
		injectInto(persistenceMapper);

		final ObjectMapper communicationMapper = communicationMapperSupplier.get();
		injectInto(communicationMapper);

		final Worker worker =
				new Worker(queryThreadPoolDefinition, storage, jobsThreadPool, failOnError, entityBucketSize, persistenceMapper, communicationMapper, secondaryIdSubPlanRetention);

		addWorker(worker);

		return worker;
	}

	public Worker createWorker(Dataset dataset, StoreFactory storageConfig, @NonNull String name, Validator validator, boolean failOnError) {

		final ObjectMapper persistenceMapper = persistenceMapperSupplier.get();
		injectInto(persistenceMapper);

		final ObjectMapper communicationMapper = communicationMapperSupplier.get();
		injectInto(communicationMapper);

		final Worker
				worker =
				Worker.newWorker(dataset, queryThreadPoolDefinition, jobsThreadPool, storageConfig, name, validator, failOnError, entityBucketSize, persistenceMapper, communicationMapper, secondaryIdSubPlanRetention);

		addWorker(worker);

		return worker;
	}

	private void addWorker(Worker worker) {
		nextWorker.incrementAndGet();
		workers.put(worker.getStorage().getDataset().getId(), worker);
	}

	public Worker getWorker(DatasetId worker) {
		return Objects.requireNonNull(workers.get(worker));
	}


	public Collection<Worker> getWorkers() {
		return Collections.unmodifiableCollection(workers.values());
	}

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) {
		if (!workers.containsKey(dataset)) {
			throw new NoSuchElementException(String.format("Did not find Dataset[%s] in [%s]", dataset, workers.keySet()));
		}

		return workers.get(dataset).getStorage().getCentralRegistry();
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		return null; // Workers simply have no MetaRegistry.
	}

	public void removeWorkerFor(DatasetId dataset) {
		final Worker worker = workers.get(dataset);

		/*
		 Close the job manager first, so all jobs are done and none can be added, when the worker is
		 removed from dataset2Worker (which is used in deserialization of NamespacedIds, i.e. content of ForwardToWorkerMessages)
		 */
		worker.getJobManager().close();

		final Worker removed = workers.remove(dataset);
		if (removed == null) {
			return;
		}

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
