package com.bakdata.conquery.models.worker;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

	private final int entityBucketSize;

	
	public Workers(ThreadPoolDefinition queryThreadPoolDefinition, int jobThreadPoolSize, int entityBucketSize) {
		this.queryThreadPoolDefinition = queryThreadPoolDefinition;
		
		// TODO: 30.06.2020 build from configuration
		jobsThreadPool = new ThreadPoolExecutor(jobThreadPoolSize / 2, jobThreadPoolSize,
												60L, TimeUnit.SECONDS,
												new LinkedBlockingQueue<>(),
												new ThreadFactoryBuilder().setNameFormat("Workers Helper %d").build()
		);
		this.entityBucketSize = entityBucketSize;

		jobsThreadPool.prestartAllCoreThreads();
	}

	public Worker createWorker(WorkerStorage storage, boolean failOnError) {
		final Worker worker = Worker.newWorker(queryThreadPoolDefinition, jobsThreadPool, storage, failOnError, entityBucketSize);

		addWorker(worker);

		return worker;
	}

	public Worker createWorker(Dataset dataset, StorageFactory storageConfig, @NonNull String storagePrefix, @NonNull String name, Validator validator, boolean failOnError) {
		final Worker worker = Worker.newWorker(dataset, queryThreadPoolDefinition, jobsThreadPool, storageConfig, storagePrefix, name, validator, failOnError, entityBucketSize);

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
