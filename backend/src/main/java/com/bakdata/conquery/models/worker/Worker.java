package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.validation.Validator;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.io.mina.NetworkSession;
import com.bakdata.conquery.io.xodus.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.config.ThreadPoolDefinition;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.network.MessageToManagerNode;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToNamespace;
import com.bakdata.conquery.models.query.QueryExecutor;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
	@Setter
	private NetworkSession session;
	/**
	 * Pool that can be used in Jobs to execute a job in parallel.
	 */
	@Getter
	private final ExecutorService executorService;
	@Getter 
	private final BucketManager bucketManager;
	
	
	private Worker(
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull WorkerStorage storage,
		@NonNull ExecutorService executorService,
		boolean failOnError
		) {
		this.jobManager = new JobManager(storage.getWorker().getName(), failOnError);
		this.storage = storage;
		this.queryExecutor = new QueryExecutor(queryThreadPoolDefinition.createService("QueryExecutor %d"));
		this.executorService = executorService;
		this.bucketManager = BucketManager.create(this, storage);
		
	}

	public static Worker newWorker(
			@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
			@NonNull ExecutorService executorService,
			@NonNull WorkerStorage storage,
			boolean failOnError,
			int entityBucketSize) {

		return new Worker(queryThreadPoolDefinition, storage, executorService, failOnError);
	}

	public static Worker newWorker(
		@NonNull Dataset dataset,
		@NonNull ThreadPoolDefinition queryThreadPoolDefinition,
		@NonNull ExecutorService executorService,
		@NonNull StorageFactory config,
		@NonNull String storagePrefix,
		@NonNull String directory,
		@NonNull Validator validator,
		boolean failOnError,
		int entityBucketSize) {

		WorkerStorage workerStorage = config.createWorkerStorage(validator, List.of(storagePrefix,directory));
		if (workerStorage == null) {
			throw new IllegalStateException(String.format("Cannot create a new worker %s, because the storage directory already exists: %s", dataset, directory));
		}


		WorkerInformation info = new WorkerInformation();
		info.setDataset(dataset.getId());
		info.setName(directory);
		info.setEntityBucketSize(entityBucketSize);

		workerStorage.loadData();
		workerStorage.updateDataset(dataset);
		workerStorage.setWorker(info);

		return new Worker(queryThreadPoolDefinition, workerStorage, executorService, failOnError);
	}
	
	public ModificationShieldedWorkerStorage getStorage() {
		return new ModificationShieldedWorkerStorage(storage);
	}
	
	public WorkerInformation getInfo() {
		return storage.getWorker();
	}
	
	public Int2ObjectMap<Entity> getEntities(){
		return bucketManager.getEntities();
	}

	@Override
	public NetworkSession getMessageParent() {
		return session;
	}

	@Override
	public MessageToManagerNode transform(NamespaceMessage message) {
		return new ForwardToNamespace(getInfo().getDataset(), message);
	}

	public ObjectMapper inject(ObjectMapper binaryMapper) {
		return new SingletonNamespaceCollection(storage.getCentralRegistry()).injectInto(binaryMapper);
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

	public void addImport(Import imp) {
		storage.addImport(imp);
	}

	public void removeImport(ImportId importId) {
		final Import imp = storage.getImport(importId);

		for (DictionaryId dictionaryId : imp.getDictionaries()) {
			storage.removeDictionary(dictionaryId);
		}

		storage.removeImport(importId);
		bucketManager.removeImport(importId);
	}

	public void addBucket(Bucket bucket) {
		bucketManager.addBucket(bucket);
	}

	public void removeConcept(ConceptId conceptId) {
		bucketManager.removeConcept(conceptId);
	}

	public void updateConcept(Concept<?> concept) {
		bucketManager.removeConcept(concept.getId());
		bucketManager.addConcept(concept);
	}

	public void updateDataset(Dataset dataset) {
		storage.updateDataset(dataset);
	}

	public void updateDictionary(Dictionary dictionary) {
		storage.updateDictionary(dictionary);
	}

	public void updateWorkerInfo(WorkerInformation info) {
		storage.updateWorker(info);
	}

	@SneakyThrows
	public void remove() {
		close();
		storage.remove();
	}

	public void addTable(Table table) {
		storage.addTable(table);
	}

	public void removeTable(TableId table) {
		storage.removeTable(table);
	}

	public void addSecondaryId(SecondaryIdDescription secondaryId) {
		storage.addSecondaryId(secondaryId);
	}

	public void removeSecondaryId(SecondaryIdDescriptionId secondaryId) {
		storage.removeSecondaryId(secondaryId);
	}
}