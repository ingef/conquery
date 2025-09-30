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
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Worker implements MessageSender.Transforming<NamespaceMessage, NetworkMessage<?>>, Closeable {
	// Making this private to have more control over adding and deleting and keeping a consistent state
	@Getter(AccessLevel.NONE)
	private WorkerStorage storage;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	private ExecutorService jobsExecutorService;

	private JobManager jobManager;

	private QueryExecutor queryExecutor;
	private BucketManager bucketManager;
	@Setter
	private NetworkSession session;

	/**
	 * @implSpec storage must not be open yet.
	 */
	public static Worker create(
			@NonNull WorkerStorage storage, ShardWorkers shardWorkers, @NonNull ThreadPoolDefinition queryThreadPoolDefinition,
			@NonNull ExecutorService jobsExecutorService,
			boolean failOnError,
			int secondaryIdSubPlanLimit, ObjectMapper persistenceMapper, boolean loadStorage) {


		final Worker worker = new Worker();

		worker.storage = storage;
		worker.jobsExecutorService = jobsExecutorService;
		worker.queryExecutor = new QueryExecutor(worker, queryThreadPoolDefinition.createService("QueryExecutor %d"), secondaryIdSubPlanLimit);

		// The order of the remaining code cannot be changed, there are dependencies throughout.
		storage.openStores(persistenceMapper);
		storage.loadKeys();

		shardWorkers.addWorker(worker);

		if (loadStorage) {
			storage.loadData();
		}

		worker.jobManager = new JobManager(storage.getWorker().getName(), failOnError);

		// BucketManager.create loads NamespacedStorage keys
		worker.bucketManager = BucketManager.create(worker, storage);

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