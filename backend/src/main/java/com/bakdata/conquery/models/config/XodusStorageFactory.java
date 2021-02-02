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
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
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
	public NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting) {
		File storageDir = getDirectory().resolve(pathName.stream().collect(Collectors.joining("/"))).toFile();
		if (returnNullOnExisting && storageDir.exists()) {
			return null;
		}
		return new NamespaceStorageXodus(validator, storageDir, this);
	}

	@Override
	public WorkerStorage createWorkerStorage(Validator validator, List<String> pathName, boolean returnNullOnExisting) {
		File storageDir = getDirectory().resolve(pathName.stream().collect(Collectors.joining("/"))).toFile();

		if (returnNullOnExisting && storageDir.exists()) {
			return null;
		}
		return new WorkerStorageXodus(validator, storageDir, this);
	}

	@Override
	@SneakyThrows
	public Queue<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode) {
		Path baseDir = getDirectory().resolve(managerNode.isUseNameForStoragePrefix() ? managerNode.getName() : "");

		if(baseDir.toFile().mkdirs()){
			log.warn("Had to create Storage Dir at `{}`", getDirectory());
		}

		ConcurrentLinkedQueue<NamespaceStorage> storages = new ConcurrentLinkedQueue<>();

		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.toFile().listFiles((file, name) -> name.startsWith("dataset_"))) {
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
			log.debug("Still waiting for Datasets to load. {} already finished.", managerNode.getDatasetRegistry().getDatasets());
		}

		log.info("All stores loaded: {}",  managerNode.getDatasetRegistry().getDatasets());
		return storages;
	}

	@Override
	@SneakyThrows
	public Queue<WorkerStorage> loadWorkerStorages(ShardNode shardNode) {
		Path baseDir = getDirectory().resolve(shardNode.isUseNameForStoragePrefix() ? shardNode.getName() : "");

		if(baseDir.toFile().mkdirs()){
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}


		ConcurrentLinkedQueue<WorkerStorage> storages = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.toFile().listFiles((file, name) -> name.startsWith("worker_"))) {

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
}
