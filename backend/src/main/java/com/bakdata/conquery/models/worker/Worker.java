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
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MasterMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	@Getter
	private final JobManager jobManager;
	@Getter
	private final WorkerStorage storage;
	@Getter
	private final QueryExecutor queryExecutor;
	@Getter
	private final WorkerInformation info;
	@Setter
	private NetworkSession session;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService executorService;
	
	
	public Worker(
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull ExecutorService executorService,
		@NonNull WorkerStorage storage
		) {
		this(
			new JobManager(storage.getWorker().getName()),
			storage,
			new QueryExecutor(queryThreadPoolDefinition.createService("QueryExecutor %d")),
			storage.getWorker(),
			executorService);
		storage.setBucketManager(new BucketManager(this.jobManager, this.storage, this.info));
		
	}
	
	public static Worker newWorker(
		@NonNull WorkerInformation info,
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull ExecutorService executorService,
		@NonNull StorageConfig config,
		@NonNull File directory,
		@NonNull Validator validator) {

		WorkerStorage workerStorage = WorkerStorage.tryLoad(validator, config, directory);
		if (workerStorage != null) {
			throw new IllegalStateException(String.format("Cannot create a new worker %s, becaus the storage directory already exists: %s", info, directory));
		}
		
		workerStorage = new WorkerStorageImpl(validator, config, directory);
		workerStorage.loadData();
		workerStorage.updateDataset(info.getDataset());
		
		workerStorage.setWorker(info);

		return new Worker(queryThreadPoolDefinition, executorService, workerStorage);
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MasterMessage transform(NamespaceMessage message) {
		return new ForwardToNamespace(info.getDataset().getId(), message);
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
		return "Worker[" + info.getId() + ", " + session.getLocalAddress() + "]";
	}
	public boolean isBusy() {
		return queryExecutor.isBusy();
	}
}