package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	// Making this private to have more control over adding and deleting and keeping a consistent state
	@Getter(AccessLevel.NONE)
	private final WorkerStorage storage;

	private final JobManager jobManager;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	private final ExecutorService jobsExecutorService;
	private final QueryExecutor queryExecutor;
	private BucketManager bucketManager;
	@Setter
	private NetworkSession session;

	public static Worker create(
			@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
			@NonNull WorkerStorage storage,
			@NonNull ExecutorService jobsExecutorService,
			boolean failOnError,
			int entityBucketSize,
			ObjectMapper persistenceMapper,
			int secondaryIdSubPlanLimit,
			boolean loadStorage,
			ShardWorkers shardWorkers
	) {
		storage.openStores(persistenceMapper);

		storage.loadKeys();

		if (loadStorage) {
			storage.loadData();
		}

		Worker worker = new Worker(storage,
								   new JobManager(storage.getWorker().getName(), failOnError),
								   jobsExecutorService,
								   new QueryExecutor(queryThreadPoolDefinition.createService("QueryExecutor %d"), secondaryIdSubPlanLimit)
		);
		shardWorkers.addWorker(worker);

		// BucketManager.create loads NamespacedStorage, which requires the WorkerStorage to be registered.
		worker.bucketManager = BucketManager.create(worker, storage, entityBucketSize);

		return worker;
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