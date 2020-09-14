package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.validation.Validator;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.io.xodus.WorkerStorageImpl;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	@Getter
	private final JobManager jobManager;
	@Getter
	private final WorkerStorage storage;
	@Getter
	private final QueryExecutor queryExecutor;
	@Setter
	private NetworkSession session;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService executorService;
	
	
	private Worker(
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull WorkerStorage storage,
		@NonNull ExecutorService executorService
		) {
		this.jobManager = new JobManager(storage.getWorker().getName());
		this.storage = storage;
		this.queryExecutor = new QueryExecutor(queryThreadPoolDefinition.createService("QueryExecutor %d"));
		this.executorService = executorService;
		
		storage.setBucketManager(new BucketManager(this.jobManager, this.storage, getInfo()));
		
	}
	
	public static Worker newWorker(
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull ExecutorService executorService,
		@NonNull WorkerStorage storage) {
			
		return new Worker(queryThreadPoolDefinition, storage, executorService);
	}
	
	public static Worker newWorker(
		@NonNull Dataset dataset,
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull ExecutorService executorService,
		@NonNull StorageConfig config,
		@NonNull File directory,
		@NonNull Validator validator) {

		WorkerStorage workerStorage = WorkerStorage.tryLoad(validator, config, directory);
		if (workerStorage != null) {
			throw new IllegalStateException(String.format("Cannot create a new worker %s, because the storage directory already exists: %s", dataset, directory));
		}
		

		WorkerInformation info = new WorkerInformation();
		info.setDataset(dataset.getId());
		info.setIncludedBuckets(new IntArrayList());
		info.setName(directory.getName());
		
		workerStorage = new WorkerStorageImpl(validator, config, directory);
		workerStorage.loadData();
		workerStorage.updateDataset(dataset);
		workerStorage.setWorker(info);

		return new Worker(queryThreadPoolDefinition, workerStorage, executorService);
	}
	
	public WorkerInformation getInfo() {
		return storage.getWorker();
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MasterMessage transform(NamespaceMessage message) {
		return new ForwardToNamespace(getInfo().getDataset(), message);
	}
	
	@Override
	public void close() {
		// We do not close the executorService here because it does not belong to this class
		try {
			queryExecutor.close();
		}
		catch (IOException e) {
			log.error("Unable to close worker query executor of {}.", this, e);
		}
		
		try {
			jobManager.close();
		}catch (Exception e) {
			log.error("Unable to close worker query executor of {}.", this, e);
		}
		
		try {
			storage.close();
		}
		catch (IOException e) {
			log.error("Unable to close worker storage of {}.", this, e);
		}
	}
	
	@Override
	public String toString() {
		return "Worker[" + getInfo().getId() + ", " + session.getLocalAddress() + "]";
	}
	public boolean isBusy() {
		return queryExecutor.isBusy();
	}
}