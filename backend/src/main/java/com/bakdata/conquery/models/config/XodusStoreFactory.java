package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreMappings.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.BigStore;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.storage.xodus.stores.StoreInfo;
import com.bakdata.conquery.io.storage.xodus.stores.WeakCachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
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
import com.google.common.collect.Sets;
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

	public static final Set<String> NAMESPACED_STORES = Set.of(
			DATASET.storeInfo().getName(),
			SECONDARY_IDS.storeInfo().getName(),
			TABLES.storeInfo().getName(),
			DICTIONARIES.storeInfo().getName() + BigStore.META,
			DICTIONARIES.storeInfo().getName() + BigStore.DATA,
			IMPORTS.storeInfo().getName(),
			CONCEPTS.storeInfo().getName());

	public static final Set<String> NAMESPACE_STORES = Sets.union(
			NAMESPACED_STORES,
			Set.of(
					ID_MAPPING.storeInfo().getName() + BigStore.META,
					ID_MAPPING.storeInfo().getName() + BigStore.DATA,
					STRUCTURE.storeInfo().getName(),
					WORKER_TO_BUCKETS.storeInfo().getName(),
					PRIMARY_DICTIONARY.storeInfo().getName()
			));
	public static final Set<String> WORKER_STORES = Sets.union(
			NAMESPACED_STORES,
			Set.of(
					WORKER.storeInfo().getName(),
					BUCKETS.storeInfo().getName(),
					C_BLOCKS.storeInfo().getName()
			));

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
	@Nullable
	private File unreadableDataDumpDirectory = null;

	@JsonIgnore
	private transient Validator validator;

	@JsonIgnore
	private transient ObjectMapper objectMapper = Jackson.BINARY_MAPPER.copy();

	@JsonIgnore
	private final BiMap<File, Environment> activeEnvironments = HashBiMap.create();

	@JsonIgnore
	private final transient Multimap<Environment, jetbrains.exodus.env.Store>
			openStoresInEnv =
			Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

	@Override
	public void init(ManagerNode managerNode) {
		validator = managerNode.getValidator();
		objectMapper = managerNode.getEnvironment().getObjectMapper();
		configureMapper(managerNode.getConfig());
	}

	@Override
	public void init(ShardNode shardNode) {
		validator = shardNode.getValidator();
		objectMapper = shardNode.getEnvironment().getObjectMapper();
		configureMapper(shardNode.getConfig());
	}

	private void configureMapper(ConqueryConfig config) {
		config.configureObjectMapper(objectMapper);
		objectMapper.setConfig(objectMapper.getDeserializationConfig().withView(InternalOnly.class));
		objectMapper.setConfig(objectMapper.getSerializationConfig().withView(InternalOnly.class));
	}

	@Override
	@SneakyThrows
	public Collection<NamespaceStorage> loadNamespaceStorages() {
		return loadNamespacedStores("dataset_", (storesToTest) -> new NamespaceStorage(validator, this, storesToTest), NAMESPACE_STORES);
	}

	@Override
	@SneakyThrows
	public Collection<WorkerStorage> loadWorkerStorages() {
		return loadNamespacedStores("worker_", (storesToTest) -> new WorkerStorage(validator, this, storesToTest), WORKER_STORES);
	}


	private <T extends NamespacedStorage> Queue<T> loadNamespacedStores(String prefix, Function<String, T> creator, Set<String> storesToTest)
			throws InterruptedException {
		File baseDir = getDirectory().toFile();

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}

		Queue<T> storages = new ConcurrentLinkedQueue<>();
		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : Objects.requireNonNull(baseDir.listFiles((file, name) -> file.isDirectory() && name.startsWith(prefix)))) {

			final String name = directory.getName();

			loaders.submit(() -> {
				try {
					ConqueryMDC.setLocation(directory.toString());

					if (!environmentHasStores(directory, storesToTest)) {
						log.warn("No valid WorkerStorage found.");
						return;
					}

					T namespacedStorage = creator.apply(name);
					log.debug("BEGIN reading Storage");
					namespacedStorage.loadData();

					storages.add(namespacedStorage);

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

	private boolean environmentHasStores(File pathName, Set<String> storesToTest) {
		Environment env = findEnvironment(pathName);
		boolean exists = env.computeInTransaction(t -> {
			final List<String> allStoreNames = env.getAllStoreNames(t);
			final boolean complete = allStoreNames.containsAll(storesToTest);
			if(complete) {
				log.trace("Storage contained all stores: {}", storesToTest);
				return true;
			}
			if(log.isWarnEnabled()) {
				final HashSet<String> missing = Sets.newHashSet(storesToTest);
				allStoreNames.forEach(missing::remove);
				log.warn("Storage did not contain all required stores. It is missing: {}. It had {}", missing, allStoreNames);
			}
			return false;
		});
		if (!exists) {
			closeEnvironment(env);
		}
		return exists;
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(String pathName) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, DATASET));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, TABLES), centralRegistry);
	}

	@Override
	public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName) {
		final Environment environment = findEnvironment(pathName);

		final SingletonNamespaceCollection namespaceCollection = new SingletonNamespaceCollection(centralRegistry);

		final BigStore<IId<Dictionary>, Dictionary> bigStore;

		synchronized (openStoresInEnv) {
			bigStore =
					new BigStore<>(
							this,
							validator,
							environment,
							DICTIONARIES.storeInfo(),
							this::closeStore,
							this::removeStore,
							namespaceCollection.injectIntoNew(objectMapper)
					);
		}

		if (useWeakDictionaryCaching) {
			return StoreMappings.identifiableCachedStore(new WeakCachedStore<>(bigStore, getWeakCacheDuration()), centralRegistry);
		}
		return StoreMappings.identifiable(bigStore, centralRegistry);
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS), centralRegistry);
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS), centralRegistry);
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS), centralRegistry);
	}

	@Override
	public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER));
	}

	@Override
	public SingletonStore<EntityIdMap> createIdMappingStore(String pathName) {
		final Environment environment = findEnvironment(pathName);

		synchronized (openStoresInEnv) {
			final BigStore<Boolean, EntityIdMap> bigStore =
					new BigStore<>(this, validator, environment, ID_MAPPING.storeInfo(), this::closeStore, this::removeStore, objectMapper);

			return new SingletonStore<>(new CachedStore<>(bigStore));
		}
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, SingletonNamespaceCollection centralRegistry) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE), centralRegistry);
	}

	@Override
	public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "executions")), validator, EXECUTIONS), centralRegistry);
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, String pathName) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "formConfigs")), validator, FORM_CONFIG), centralRegistry);
	}

	@Override
	public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "users")), validator, AUTH_USER), centralRegistry);
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "roles")), validator, AUTH_ROLE), centralRegistry);
	}


	@Override
	public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "groups")), validator, AUTH_GROUP), centralRegistry);
	}

	@Override
	public SingletonStore<Dictionary> createPrimaryDictionaryStore(String pathName, SingletonNamespaceCollection namespaceCollection) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, PRIMARY_DICTIONARY), namespaceCollection);
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

	private void closeStore(jetbrains.exodus.env.Store store) {
		Environment env = store.getEnvironment();
		Collection<jetbrains.exodus.env.Store> stores = openStoresInEnv.get(env);
		stores.remove(store);
		log.info("Closed XodusStore: {}", this);

		if (!stores.isEmpty()) {
			return;
		}
		log.info("Closed last XodusStore in Environment. Closing Environment as well: {}", env.getLocation());

		closeEnvironment(env);
	}

	private void closeEnvironment(Environment env) {
		synchronized (activeEnvironments) {

			if (activeEnvironments.remove(activeEnvironments.inverse().get(env)) == null) {
				return;
			}
			env.close();
		}
	}

	private void removeStore(jetbrains.exodus.env.Store store) {
		Environment env = store.getEnvironment();
		Collection<jetbrains.exodus.env.Store> stores = openStoresInEnv.get(env);

		if (!stores.isEmpty()) {
			return;
		}

		log.info("Removed last XodusStore in Environment. Removing Environment as well: {}", env.getLocation());
		env.close();
	}

	public static void removeEnvironmentHook(Environment env) {
		log.info("Deleting Environment[{}]", env.getLocation());
			try {
				FileUtil.deleteRecursive(Path.of(env.getLocation()));
			}
			catch (IOException e) {
				log.error("Cannot delete directory of removed Environment[{}]", env.getLocation(), e);
			}
		}

		public <KEY, VALUE > Store < KEY, VALUE > createStore(Environment environment, Validator validator, StoreMappings storeId){
			final StoreInfo<KEY, VALUE> storeInfo = storeId.storeInfo();
			synchronized (openStoresInEnv) {
				return new CachedStore<>(
						new SerializingStore<>(

								new XodusStore(environment, storeInfo.getName(), this::closeStore, this::removeStore),
								validator,
								objectMapper.copy(),
								storeInfo.getKeyType(),
								storeInfo.getValueType(),
								this.isValidateOnWrite(),
								this.isRemoveUnreadableFromStore(),
								this.getUnreadableDataDumpDirectory()
							));
			}
		}

	}
