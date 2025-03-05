package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.WorkerStorageImpl;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	// Making this private to have more control over adding and deleting and keeping a consistent state
	private final WorkerStorage storage;

	@Getter
	private final JobManager jobManager;
	@Getter
	private final QueryExecutor queryExecutor;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService jobsExecutorService;
	@Getter
	private final BucketManager bucketManager;
	@Setter
	private NetworkSession session;


	public Worker(
			@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
			@NonNull WorkerStorage storage,
			@NonNull ExecutorService jobsExecutorService,
			boolean failOnError,
			int entityBucketSize,
			ObjectMapper persistenceMapper,
			int secondaryIdSubPlanLimit,
			boolean loadStorage
	) {
		this.storage = storage;
		this.jobsExecutorService = jobsExecutorService;


		storage.openStores(persistenceMapper);

		storage.loadKeys();

		if (loadStorage) {
			storage.loadData();
		}

		jobManager = new JobManager(storage.getWorker().getName(), failOnError);
		queryExecutor = new QueryExecutor(this, queryThreadPoolDefinition.createService("QueryExecutor %d"), secondaryIdSubPlanLimit);
		bucketManager = BucketManager.create(this, storage, entityBucketSize);
	}

	@SneakyThrows(IOException.class)
	public static Worker newWorker(
			@NonNull Dataset dataset,
			@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
			@NonNull ExecutorService jobsExecutorService,
			@NonNull StoreFactory config,
			@NonNull String directory,
			boolean failOnError,
			int entityBucketSize,
			InternalMapperFactory internalMapperFactory,
			int secondaryIdSubPlanLimit) {

		WorkerStorageImpl workerStorage = new WorkerStorageImpl(config, directory);
		final ObjectMapper persistenceMapper = internalMapperFactory.createWorkerPersistenceMapper(workerStorage);
		workerStorage.openStores(persistenceMapper);

		dataset.setNamespacedStorageProvider(workerStorage);

		// On the worker side we don't have to set the object writer for ForwardToWorkerMessages in WorkerInformation
		WorkerInformation info = new WorkerInformation();
		info.setDataset(dataset.getId());
		info.setName(directory);
		info.setEntityBucketSize(entityBucketSize);
		workerStorage.updateDataset(dataset);
		workerStorage.setWorker(info);
		workerStorage.close();


		return new Worker(queryThreadPoolDefinition, workerStorage, jobsExecutorService, failOnError, entityBucketSize, persistenceMapper, secondaryIdSubPlanLimit,
						  config.isLoadStoresOnStart());
	}

	public ModificationShieldedWorkerStorage getStorage() {
		return new ModificationShieldedWorkerStorage(storage);
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MessageToManagerNode transform(NamespaceMessage message) {
		return new ForwardToNamespace(getInfo().getDataset(), message);
	}

	public WorkerInformation getInfo() {
		return storage.getWorker();
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
		}
		catch (Exception e) {
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
		return "Worker[" + getInfo().getId() + ", " + (session != null ? session.getLocalAddress() : "no session") + "]";
	}

	public boolean isBusy() {
		return queryExecutor.isBusy() || jobManager.isSlowWorkerBusy();
	}

	public void addImport(Import imp) {
		storage.addImport(imp);
	}

	public void removeImport(ImportId imp) {
		bucketManager.removeImport(imp);
	}

	public void addBucket(Bucket bucket) {
		bucketManager.addBucket(bucket);
	}

	public void removeConcept(Concept<?> conceptId) {
		bucketManager.removeConcept(conceptId);
	}

	public void updateConcept(Concept<?> concept) {
		bucketManager.updateConcept(concept);
	}

	public void updateDataset(Dataset dataset) {
		storage.updateDataset(dataset);
	}

	public void updateWorkerInfo(WorkerInformation info) {
		storage.updateWorker(info);
	}

	@SneakyThrows
	public void remove() {
		try {
			queryExecutor.close();
		}
		catch (IOException e) {
			log.error("Unable to close worker query executor of {}.", this, e);
		}

		try {
			jobManager.close();
		}
		catch (Exception e) {
			log.error("Unable to close worker query executor of {}.", this, e);
		}

		// Don't call close() here because, it would try to close a removed store or remove a closed store
		storage.removeStorage();
	}

	public void addTable(Table table) {
		storage.addTable(table);
	}

	public void removeTable(TableId table) {
		bucketManager.removeTable(table);
	}

	public void addSecondaryId(SecondaryIdDescription secondaryId) {
		storage.addSecondaryId(secondaryId);
	}

	public void removeSecondaryId(SecondaryIdDescriptionId secondaryId) {
		storage.removeSecondaryId(secondaryId);
	}
}