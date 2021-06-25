package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreInfo.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreInfo;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.BigStore;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.storage.xodus.stores.WeakCachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import io.dropwizard.util.Duration;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@With
@CPSType(id = "XODUS", base = StoreFactory.class)
public class XodusStoreFactory implements StoreFactory {

    private Path directory = Path.of("storage");

    private boolean validateOnWrite = false;
    @NotNull
    @Valid
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

    @JsonIgnore
    private transient Validator validator;

    @JsonIgnore
    private transient ObjectMapper objectMapper = Jackson.BINARY_MAPPER.copy();

    @JsonIgnore
    private BiMap<File, Environment> activeEnvironments = HashBiMap.create();

    @JsonIgnore
    private final transient Multimap<Environment, jetbrains.exodus.env.Store> openStoresInEnv = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

    @Override
    public void init(ManagerNode managerNode) {
        validator = managerNode.getValidator();
        configureMapper(managerNode.getConfig());
    }

    @Override
    public void init(ShardNode shardNode) {
        validator = shardNode.getValidator();
        configureMapper(shardNode.getConfig());
    }

    private void configureMapper(ConqueryConfig config) {
        config.configureObjectMapper(objectMapper);
    }

    @Override
    @SneakyThrows
    public Collection<NamespaceStorage> loadNamespaceStorages() {
		return loadNamespacedStores("dataset_", (elements) -> new NamespaceStorage(validator, this, elements));
    }

    @Override
    @SneakyThrows
    public Collection<WorkerStorage> loadWorkerStorages() {
		return loadNamespacedStores("worker_", (elements) -> new WorkerStorage(validator, this, elements));
    }


	private <T extends NamespacedStorage> Queue<T> loadNamespacedStores(String prefix, Function<String, T> creator)
			throws InterruptedException {
		File baseDir = getDirectory().toFile();

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}

		Queue<T> storages = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : baseDir.listFiles((file, name) -> file.isDirectory()  && name.startsWith(prefix))) {

			final String name = directory.getName();

			loaders.submit(() -> {
				try {
					ConqueryMDC.setLocation(directory.toString());

					if (!environmentHasStores(directory)) {
						log.warn("No valid WorkerStorage found.");
						return;
					}

					T workerStorage = creator.apply(name);
					log.debug("BEGIN reading Storage");
					workerStorage.loadData();

					storages.add(workerStorage);

				}
				catch (Exception e) {
					log.error("Failed reading Storage", e);
				}
				finally {
					log.debug("DONE reading Storage");
					ConqueryMDC.clearLocation();
				}
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {


			log.debug("Waiting for Worker storages to load. {} are already finished.", storages.size());
		}

		log.info("All WorkerStores loaded: {}", storages);
		return storages;
	}

	private List<String> getRelativePathElements(Path path) {
        ArrayList<String> list = new ArrayList<>();
        Path relative = getDirectory().relativize(path);
        for (int i = 0; i < relative.getNameCount(); i++) {
            list.add(relative.getName(i).toString());
        }
        return list;
    }

    private boolean environmentHasStores(File pathName) {
        Environment env = findEnvironment(pathName);
        boolean exists = env.computeInTransaction(t -> env.storeExists(StoreInfo.DATASET.getName(), t));
        env.computeInTransaction(t -> env.getAllStoreNames(t));
        if (!exists) {
            closeEnvironment(env);
        }
        return exists;
    }

    @Override
    public SingletonStore<Dataset> createDatasetStore(String pathName) {
        return DATASET.singleton(createStore(findEnvironment(pathName), validator, DATASET));
    }

    @Override
    public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName) {
        return SECONDARY_IDS.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS), centralRegistry);
    }

    @Override
    public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName) {
        return TABLES.identifiable(createStore(findEnvironment(pathName), validator, TABLES), centralRegistry);
    }

	@Override
	public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName) {
		final Environment environment = findEnvironment(pathName);

		final SingletonNamespaceCollection namespaceCollection = new SingletonNamespaceCollection(centralRegistry);

		final BigStore<IId<Dictionary>, Dictionary> bigStore;

		synchronized (openStoresInEnv) {
			bigStore = new BigStore<>(this, validator, environment, DICTIONARIES, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment, namespaceCollection.injectInto(objectMapper));
		}

		final Store<IId<Dictionary>, Dictionary> result;

		if (useWeakDictionaryCaching) {
			result = new WeakCachedStore<>(bigStore, getWeakCacheDuration());
		}
		else {
			result = DICTIONARIES.cached(bigStore);
		}

		return DICTIONARIES.identifiableCachedStore(result, centralRegistry);
	}

    @Override
    public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName) {
        return CONCEPTS.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS), centralRegistry);
    }

    @Override
    public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName) {
        return IMPORTS.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS), centralRegistry);
    }

    @Override
    public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName) {
        return C_BLOCKS.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS), centralRegistry);
    }

    @Override
    public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName) {
        return BUCKETS.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS), centralRegistry);
    }

    @Override
    public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName) {
        return WORKER.singleton(createStore(findEnvironment(pathName), validator, WORKER));
    }

	@Override
	public SingletonStore<PersistentIdMap> createIdMappingStore(String pathName) {
		final Environment environment = findEnvironment(pathName);

		synchronized (openStoresInEnv) {
			final BigStore<Boolean, PersistentIdMap> bigStore =
					new BigStore<>(this, validator, environment, ID_MAPPING, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment, objectMapper);

			return new SingletonStore<>(new CachedStore<>(bigStore));
		}
	}

    @Override
    public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName) {
        return WORKER_TO_BUCKETS.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS));
    }

    @Override
    public SingletonStore<StructureNode[]> createStructureStore(String pathName, SingletonNamespaceCollection centralRegistry) {
        return STRUCTURE.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE), centralRegistry);
    }

    @Override
    public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName) {
        return EXECUTIONS.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "executions")), validator, EXECUTIONS), centralRegistry, datasetRegistry);
    }

    @Override
    public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName) {
        return FORM_CONFIG.identifiable(createStore(findEnvironment(resolveSubDir(pathName,  "formConfigs")), validator, FORM_CONFIG), centralRegistry, datasetRegistry);
    }

    @Override
    public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_USER.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "users")), validator, AUTH_USER), centralRegistry);
    }

    @Override
    public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_ROLE.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "roles")), validator, AUTH_ROLE), centralRegistry);
    }


    @Override
    public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_GROUP.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "groups")), validator, AUTH_GROUP), centralRegistry);
    }

    private File resolveSubDir(String... subdirs) {
		Path current = getDirectory();

		for (String dir : subdirs) {
			current = current.resolve(dir);
		}

		return current.toFile();
    }

    /**
     * Returns this.directory if the list is empty.
     */
	@NonNull
	@JsonIgnore
	private File getStorageDir(String pathName) {
		return getDirectory().resolve(pathName).toFile();
    }

    private Environment findEnvironment(@NonNull File path) {
        synchronized (activeEnvironments) {
            return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(path, getXodus().createConfig()));
        }
    }

    private Environment findEnvironment(String pathName) {
        synchronized (activeEnvironments) {
            File path = getStorageDir(pathName);
            return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(p, getXodus().createConfig()));
        }
    }

    private void closeEnvironment(Environment env) {
        synchronized (activeEnvironments) {
            if (env == null) {
                return;
            }

            if(activeEnvironments.remove(activeEnvironments.inverse().get(env)) == null){
                return;
            }
            env.close();
        }
    }

    private void removeEnvironment(Environment env) {
        log.info("Deleting Environment[{}]", env.getLocation());
        try {
            FileUtil.deleteRecursive(Path.of(env.getLocation()));
        }catch (IOException e) {
            log.error("Cannot delete directory of removed Environment[{}]", env.getLocation(), log.isDebugEnabled()? e : null);
        }
    }

    public <KEY, VALUE> Store<KEY, VALUE> createStore(Environment environment, Validator validator, StoreInfo storeId) {
        synchronized (openStoresInEnv) {
            return new CachedStore<>(
					new SerializingStore<>(
							this,
							new XodusStore(environment, storeId, openStoresInEnv.get(environment), this::closeEnvironment, this::removeEnvironment),
							validator,
							storeId,
							objectMapper
					));
        }
    }

}
