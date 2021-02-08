package com.bakdata.conquery.models.config;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.*;
import com.bakdata.conquery.io.xodus.stores.SerializingStore;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Setter @ToString
@CPSType(id = "XODUS", base = StorageFactory.class)
public class XodusStorageFactory implements StorageFactory {

	private Path directory = Path.of("storage");

	private boolean validateOnWrite = false;
	@NotNull @Valid
	private XodusConfig xodus = new XodusConfig();

	private boolean useWeakDictionaryCaching = true;
	@NotNull
	private Duration weakCacheDuration = Duration.hours(48);

	@Min(1)
	private int nThreads = Runtime.getRuntime().availableProcessors();
	
	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadableFromStore = false;
	
	/**
	 * When set, all values that could not be deserialized from the persistent store, are dump into individual files.
	 */
	private Optional<File> unreadableDataDumpDirectory = Optional.empty();

	@Override
	public MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets) {
		return new MetaStorageXodus(datasets, validator, this, pathName);
	}

	@Override
	public NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName) {
		File storageDir = getStorageDir(pathName);
		if (storageDir.exists()) {
			throw new IllegalStateException("Cannot create a new storage at " + pathName + ". It seems that the store already exists.");
		}
		return new NamespaceStorageXodus(validator, storageDir, this);
	}

	@Override
	public WorkerStorage createWorkerStorage(Validator validator, List<String> pathName) {
		File storageDir = getStorageDir(pathName);

		if (storageDir.exists()) {
			throw new IllegalStateException("Cannot create a new storage at " + pathName + ". It seems that the store already exists.");
		}
		return new WorkerStorageXodus(validator, storageDir, this);
	}

	@Override
	@SneakyThrows
	public Queue<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, List<String> pathName) {
		@NonNull File baseDir =  getStorageDir(pathName);

		if(baseDir.mkdirs()){
			log.warn("Had to create Storage Dir at `{}`", getDirectory());
		}

		ConcurrentLinkedQueue<NamespaceStorage> storages = new ConcurrentLinkedQueue<>();

		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.listFiles((file, name) -> name.startsWith("dataset_"))) {
			loaders.submit(() -> {
				NamespaceStorage datasetStorage = NamespaceStorageXodus.tryLoad(managerNode.getValidator(), this, directory);

				if (datasetStorage == null) {
					log.warn("Unable to load a dataset at `{}`", directory);
					return;
				}
				storages.add(datasetStorage);
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)){
			log.debug("Still waiting for Datasets to load. {} already finished.", storages);
		}

		log.info("All stores loaded: {}",  storages);
		return storages;
	}

	@Override
	@SneakyThrows
	public Queue<WorkerStorage> loadWorkerStorages(ShardNode shardNode, List<String> pathName) {
		@NonNull File baseDir = getStorageDir(pathName);

		if(baseDir.mkdirs()){
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}


		ConcurrentLinkedQueue<WorkerStorage> storages = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.listFiles((file, name) -> name.startsWith("worker_"))) {

			loaders.submit(() -> {
				ConqueryMDC.setLocation(directory.toString());

				WorkerStorage workerStorage = WorkerStorageXodus.tryLoad(shardNode.getValidator(), this, directory);
				if (workerStorage == null) {
					log.warn("No valid WorkerStorage found.");
					return;
				}

				storages.add(workerStorage);

				ConqueryMDC.clearLocation();
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for Worker storages to load. {} are already finished.", storages.size());
		}
		return storages;
	}

	@NonNull
	@JsonIgnore
	/**
	 * Returns this.directory if the list is empty.
	 */
	private File getStorageDir(List<String> pathName) {
		return getDirectory().resolve(pathName.stream().collect(Collectors.joining("/"))).toFile();
	}
}
