package com.bakdata.conquery.models.config;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.*;
import com.bakdata.conquery.io.xodus.stores.*;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
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
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.dropwizard.util.Duration;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bakdata.conquery.io.xodus.StoreInfo.*;

@Slf4j
@Getter
@Setter
@ToString
@CPSType(id = "XODUS", base = StorageFactory.class)
public class XodusStorageFactory implements StorageFactory {

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
    private Map<File,Environment> activeEnvironments = new HashMap<>();

    @JsonIgnore
    private transient Multimap<Environment, jetbrains.exodus.env.Store> openStoresInEnv = ArrayListMultimap.create();

    @Override
    public void init(ManagerNode managerNode) {
        validator = managerNode.getValidator();
    }

    @Override
    public MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets) {
        return new MetaStorageXodus(datasets, validator, this, pathName);
    }

    @Override
    @SneakyThrows
    public Collection<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, List<String> pathName) {
        @NonNull File baseDir = getStorageDir(pathName);

        if (baseDir.mkdirs()) {
            log.warn("Had to create Storage Dir at `{}`", getDirectory());
        }

        ConcurrentLinkedQueue<NamespaceStorage> storages = new ConcurrentLinkedQueue<>();

        ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


        for (File directory : baseDir.listFiles((file, name) -> name.startsWith("dataset_"))) {
            loaders.submit(() -> {
                List<String> pathElems = getPathElements(directory.toPath());
                ConqueryMDC.setLocation(directory.toString());

                if (environmentHasStores(pathElems)) {
                    log.warn("No valid WorkerStorage found.");
                    return;
                }

                NamespaceStorage namespaceStorage = new InternalNamespaceStorage(validator, this, pathElems);

                storages.add(namespaceStorage);

                ConqueryMDC.clearLocation();
            });
        }


        loaders.shutdown();
        while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
            log.debug("Still waiting for Datasets to load. {} already finished.", storages);
        }

        log.info("All stores loaded: {}", storages);
        return storages;
    }

    @Override
    @SneakyThrows
    public Collection<WorkerStorage> loadWorkerStorages(ShardNode shardNode, List<String> pathName) {
        @NonNull File baseDir = getStorageDir(pathName);

        if (baseDir.mkdirs()) {
            log.warn("Had to create Storage Dir at `{}`", baseDir);
        }


        ConcurrentLinkedQueue<WorkerStorage> storages = new ConcurrentLinkedQueue<>();
        ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


        for (File directory : baseDir.listFiles((file, name) -> name.startsWith("worker_"))) {

            loaders.submit(() -> {
                List<String> pathElems = getPathElements(directory.toPath());
                ConqueryMDC.setLocation(directory.toString());
                
                if (environmentHasStores(pathElems)) {
                    log.warn("No valid WorkerStorage found.");
                    return;
                }

                WorkerStorage workerStorage = new InternalWorkerStorage(validator, this, pathElems);

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

    private List<String> getPathElements(Path path) {
        ArrayList<String> list = new ArrayList<>();
        for( int i = 0; i < path.getNameCount(); i++) {
            list.add(path.getName(i).toString());
        }
        return list;
    }

    private boolean environmentHasStores(List<String> pathName){
        Environment env = findEnvironment(pathName);
        boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getName(), t));

        if(!exists) {
            removeEnvironment(pathName);
        }
        return exists;
    }

    @Override
    public SingletonStore<Dataset> createDatasetStore(List<String> pathName) {
        return DATASET.singleton(DATASET.cached(createStore(findEnvironment(pathName), validator, DATASET)));
    }

    @Override
    public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, List<String> pathName) {
        return SECONDARY_IDS.identifiable(SECONDARY_IDS.cached(createStore(findEnvironment(pathName), validator, SECONDARY_IDS)), centralRegistry);
    }

    @Override
    public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, List<String> pathName) {
        return TABLES.identifiable(TABLES.cached(createStore(findEnvironment(pathName), validator, TABLES)), centralRegistry);
    }

    @Override
    public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, List<String> pathName) {
        if (useWeakDictionaryCaching) {
            return StoreInfo.DICTIONARIES.identifiableCachedStore(createBigWeakStore(findEnvironment(pathName),validator,StoreInfo.DICTIONARIES),centralRegistry);
        }
        else {
            return StoreInfo.DICTIONARIES.identifiable(createBigStore(findEnvironment(pathName),validator,StoreInfo.DICTIONARIES),centralRegistry);
        }
    }

    @Override
    public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, List<String> pathName) {
        return CONCEPTS.identifiable(CONCEPTS.cached(createStore(findEnvironment(pathName), validator, CONCEPTS)), centralRegistry);
    }

    @Override
    public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, List<String> pathName) {
        return IMPORTS.identifiable(IMPORTS.cached(createStore(findEnvironment(pathName), validator, IMPORTS)), centralRegistry);
    }

    @Override
    public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, List<String> pathName) {
        return C_BLOCKS.identifiable(C_BLOCKS.cached(createStore(findEnvironment(pathName), validator, C_BLOCKS)), centralRegistry);
    }

    @Override
    public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, List<String> pathName) {
        return BUCKETS.identifiable(BUCKETS.cached(createStore(findEnvironment(pathName), validator, BUCKETS)), centralRegistry);
    }

    @Override
    public SingletonStore<WorkerInformation> createWorkerInformationStore(List<String> pathName) {
        return WORKER.singleton(WORKER.cached(createStore(findEnvironment(pathName), validator, WORKER)));
    }

    @Override
    public SingletonStore<PersistentIdMap> createIdMappingStore(List<String> pathName) {
        return ID_MAPPING.singleton(ID_MAPPING.cached(createStore(findEnvironment(pathName), validator, ID_MAPPING)));
    }

    @Override
    public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(List<String> pathName) {
        return WORKER_TO_BUCKETS.singleton(WORKER_TO_BUCKETS.cached(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS)));
    }

    @Override
    public SingletonStore<StructureNode[]> createStructureStore(List<String> pathName) {
        return STRUCTURE.singleton(STRUCTURE.cached(createStore(findEnvironment(pathName), validator, STRUCTURE)));
    }

    @Override
    public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, List<String> pathName) {
        return EXECUTIONS.identifiable(EXECUTIONS.cached(createStore(findEnvironment(appendToNewPath(pathName, "executions")), validator, EXECUTIONS)), centralRegistry, datasetRegistry);
    }

    @Override
    public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, List<String> pathName) {
        return FORM_CONFIG.identifiable(FORM_CONFIG.cached(createStore(findEnvironment(appendToNewPath(pathName, "formConfigs")), validator, FORM_CONFIG)), centralRegistry);
    }

    @Override
    public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, List<String> pathName) {
        return AUTH_USER.identifiable(AUTH_USER.cached(createStore(findEnvironment(appendToNewPath(pathName, "users")), validator, AUTH_USER)), centralRegistry);
    }

    @Override
    public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, List<String> pathName) {
        return AUTH_ROLE.identifiable(AUTH_ROLE.cached(createStore(findEnvironment(appendToNewPath(pathName, "roles")), validator, AUTH_ROLE)), centralRegistry);
    }


    @Override
    public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, List<String> pathName) {
        return AUTH_GROUP.identifiable(AUTH_GROUP.cached(createStore(findEnvironment(appendToNewPath(pathName, "roles")), validator, AUTH_GROUP)), centralRegistry);
    }

    private List<String> appendToNewPath(List<String> pathName, String users) {
        ArrayList<String> path = new ArrayList<>();
        path.addAll(pathName);
        path.add(users);
        return path;
    }

    @NonNull
    @JsonIgnore
    /**
     * Returns this.directory if the list is empty.
     */
    private File getStorageDir(List<String> pathName) {
        return getDirectory().resolve(pathName.stream().collect(Collectors.joining("/"))).toFile();
    }


    private Environment findEnvironment(List<String> pathName) {
        synchronized (activeEnvironments){
            @NonNull File path = getStorageDir(pathName);
            return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(getStorageDir(pathName), getXodus().createConfig()));
        }
    }

    private void removeEnvironment(List<String> pathName) {
        synchronized (activeEnvironments){
            @NonNull File path = getStorageDir(pathName);
            Environment env = activeEnvironments.get(path);
            if(env == null) {
                return;
            }
            env.close();
            activeEnvironments.remove(pathName);
        }
    }

    public <KEY, VALUE> Store<KEY, VALUE> createStore(Environment environment, Validator validator, StoreInfo storeId) {
        openStoresInEnv.put(environment,null);
        return new SerializingStore<KEY, VALUE>(
                this,
                new XodusStore(environment, storeId, openStoresInEnv.get(environment)),
                validator,
                storeId
        );
    }

    public <KEY, VALUE> Store<KEY, VALUE> createBigStore(Environment environment, Validator validator, StoreInfo storeId) {
        openStoresInEnv.put(environment,null);
        return storeId.cached(
                new BigStore<>(this, validator, environment, storeId, openStoresInEnv.get(environment))
        );
    }

    public <KEY, VALUE> Store<KEY, VALUE> createBigWeakStore(Environment environment, Validator validator, StoreInfo storeId) {
        openStoresInEnv.put(environment,null);
        return new WeakCachedStore<>(
                new BigStore<>(this, validator, environment, storeId, openStoresInEnv.get(environment)),
                getWeakCacheDuration()
        );
    }
}
