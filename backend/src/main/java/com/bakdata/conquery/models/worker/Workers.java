package com.bakdata.conquery.models.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.validation.Validator;

import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ShardNode} container of {@link Worker}.
 * <p>
 * Each Shard contains one {@link Worker} per {@link Dataset}.
 */
@Slf4j
public class Workers extends IdResolveContext implements Managed {
	@Getter @Setter
	private AtomicInteger nextWorker = new AtomicInteger(0);
	@Getter
	private final ConcurrentHashMap<WorkerId, Worker> workers = new ConcurrentHashMap<>();
	@JsonIgnore
	private final transient Map<DatasetId, Worker> dataset2Worker = new HashMap<>();

	/**
	 * Shared ExecutorService among Workers for Jobs.
	 */
	private final ThreadPoolExecutor jobsThreadPool;
	private final ThreadPoolDefinition queryThreadPoolDefinition;

	private final InternalMapperFactory internalMapperFactory;

	private final int entityBucketSize;

	private final int secondaryIdSubPlanRetention;

	
	public Workers(ThreadPoolDefinition queryThreadPoolDefinition, InternalMapperFactory internalMapperFactory, int entityBucketSize, int secondaryIdSubPlanRetention) {
		this.queryThreadPoolDefinition = queryThreadPoolDefinition;

		// TODO This shouldn't be coupled to the query thread pool definition
		jobsThreadPool = queryThreadPoolDefinition.createService("Workers");

		this.internalMapperFactory = internalMapperFactory;
		this.entityBucketSize = entityBucketSize;
		this.secondaryIdSubPlanRetention = secondaryIdSubPlanRetention;

		jobsThreadPool.prestartAllCoreThreads();
	}

	public Worker createWorker(WorkerStorage storage, boolean failOnError) {

		final ObjectMapper persistenceMapper = internalMapperFactory.createWorkerPersistenceMapper(this);
		final ObjectMapper communicationMapper = internalMapperFactory.createWorkerCommunicationMapper(this);

		final Worker worker =
				new Worker(queryThreadPoolDefinition, storage, jobsThreadPool, failOnError, entityBucketSize, persistenceMapper, communicationMapper, secondaryIdSubPlanRetention);

		addWorker(worker);

		return worker;
	}

	public Worker createWorker(Dataset dataset, StoreFactory storageConfig, @NonNull String name, Validator validator, boolean failOnError) {

		final ObjectMapper persistenceMapper = internalMapperFactory.createWorkerPersistenceMapper(this);

		final ObjectMapper communicationMapper = internalMapperFactory.createWorkerCommunicationMapper(this);

		final Worker
				worker =
				Worker.newWorker(dataset, queryThreadPoolDefinition, jobsThreadPool, storageConfig, name, failOnError, entityBucketSize, persistenceMapper, communicationMapper, secondaryIdSubPlanRetention);

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

	public void removeWorkerFor(DatasetId dataset) {
		final Worker worker = dataset2Worker.get(dataset);

		/*
		 Close the job manager first, so all jobs are done and none can be added, when the worker is
		 removed from dataset2Worker (which is used in deserialization of NamespacedIds, i.e. content of ForwardToWorkerMessages)
		 */
		worker.getJobManager().close();

		final Worker removed = dataset2Worker.remove(dataset);
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

	@Override
	public void start() throws Exception {

		for (Worker value : getWorkers().values()) {
			value.getJobManager().addSlowJob(new SimpleJob("Update Bucket Manager", value.getBucketManager()::fullUpdate));
		}
	}

	@Override
	public void stop() {
		jobsThreadPool.shutdown();
		for (Worker w : workers.values()) {
			w.close();
		}
	}
}
