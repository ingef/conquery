package com.bakdata.conquery.models.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

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

@Slf4j
public class Workers extends IdResolveContext {
	@Getter @Setter
	private AtomicInteger nextWorker = new AtomicInteger(0);
	@Getter
	private ConcurrentHashMap<WorkerId, Worker> workers = new ConcurrentHashMap<>();
	@JsonIgnore
	private transient Map<DatasetId, Worker> dataset2Worker = new HashMap<>();
	
	private final ThreadPoolExecutor jobsThreadPool;
	private final ThreadPoolDefinition queryThreadPoolDefinition;

	@Getter
	private final ObjectMapper mapper;
	@Getter
	private final ObjectMapper binaryMapper;

	private final int entityBucketSize;

	
	public Workers(ThreadPoolDefinition queryThreadPoolDefinition, ObjectMapper mapper, ObjectMapper binaryMapper, int entityBucketSize) {
		this.queryThreadPoolDefinition = queryThreadPoolDefinition;

		jobsThreadPool = queryThreadPoolDefinition.createService("Workers");

		this.mapper = injectInto(mapper);
		this.binaryMapper = injectInto(binaryMapper);
		this.entityBucketSize = entityBucketSize;

		jobsThreadPool.prestartAllCoreThreads();
	}

	public Worker createWorker(WorkerStorage storage, boolean failOnError) {
		final Worker worker = Worker.newWorker(queryThreadPoolDefinition, jobsThreadPool, storage, failOnError, entityBucketSize);

		addWorker(worker);

		return worker;
	}

	public Worker createWorker(Dataset dataset, StoreFactory storageConfig, @NonNull String name, Validator validator, boolean failOnError) {
		final Worker worker = Worker.newWorker(dataset, queryThreadPoolDefinition, jobsThreadPool, storageConfig, name, validator, failOnError, entityBucketSize);

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

	public void removeWorkersFor(DatasetId dataset) {
		Worker removed = dataset2Worker.remove(dataset);
		if(removed == null) {
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
