package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreMappings.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorageImpl;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.WorkerStorageImpl;
import com.bakdata.conquery.io.storage.xodus.stores.BigStore;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
import com.bakdata.conquery.io.storage.xodus.stores.EnvironmentRegistry;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.io.storage.xodus.stores.StoreInfo;
import com.bakdata.conquery.io.storage.xodus.stores.XodusStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.FileUtil;
import com.bakdata.conquery.util.validation.ValidCaffeineSpec;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
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

	/**
	 * The store names are created by hand here because the abstraction of {@link BigStore}
	 * creates two stores. Defining the expected stores like this, does not require a lot or complicated logic.
	 */
	public static final Set<String> NAMESPACED_STORES = Set.of(
			DATASET.storeInfo().getName(),
			SECONDARY_IDS.storeInfo().getName(),
			TABLES.storeInfo().getName(),
			IMPORTS.storeInfo().getName(),
			CONCEPTS.storeInfo().getName()
	);

	public static final Set<String> NAMESPACE_STORES = Sets.union(
			NAMESPACED_STORES,
			Set.of(
					ID_MAPPING.storeInfo().getName() + BigStore.META,
					ID_MAPPING.storeInfo().getName() + BigStore.DATA,
					STRUCTURE.storeInfo().getName(),
					WORKER_TO_BUCKETS.storeInfo().getName(),
					ENTITY_PREVIEW.storeInfo().getName(),
					ENTITY_TO_BUCKET.storeInfo().getName()
			)
	);
	public static final Set<String> WORKER_STORES = Sets.union(
			NAMESPACED_STORES,
			Set.of(
					WORKER.storeInfo().getName(),
					BUCKETS.storeInfo().getName(),
					C_BLOCKS.storeInfo().getName()
			)
	);
	@JsonIgnore
	private final transient Multimap<Environment, XodusStore>
			openStoresInEnv =
			Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
	private Path directory = Path.of("storage");
	private boolean validateOnWrite = false;
	@NotNull
	@Valid
	private XodusConfig xodus = new XodusConfig();
	@JsonIgnore
	private EnvironmentRegistry registry = new EnvironmentRegistry();
	/**
	 * Number of threads reading from XoduStore.
	 *
	 * @implNote it's always only one thread reading from disk, dispatching to multiple reader threads.
	 */
	@Min(1)
	private int readerWorkers = Runtime.getRuntime().availableProcessors();
	/**
	 * How many slots of buffering to use before the IO thread is put to sleep.
	 */
	@Min(1)
	private int bufferPerWorker = 20;
	@JsonIgnore
	private ExecutorService readerExecutorService;
	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadableFromStore;

	/**
	 * When set, all values that could not be deserialized from the persistent store, are dump into individual files.
	 */
	@Nullable
	private File unreadableDataDumpDirectory;

	/**
	 * If set, an environment will not be loaded if it misses a required store.
	 * If not set, the environment is loaded and the application needs to create the store.
	 * This is useful if a new version introduces a new store, but will also alter the environment upon reading.
	 */
	private boolean loadEnvironmentWithMissingStores;

	/**
	 * Cache spec for deserialized values.
	 * Conquery depends currently on <code>softValues</code> to avoid data race conditions.
	 * So a specification must include this option.
	 * See <a href="https://github.com/ben-manes/caffeine/wiki/Specification">CaffeineSpec</a>
	 */
	@NotNull
	@ValidCaffeineSpec()
	private String caffeineSpec = "softValues";

	private boolean loadStoresOnStart = false;

	/**
	 * Map of flags for each {@link StoreMappings}, whether its binary store should be fully cached.
	 * This allows for faster store access as the {@link SerializingStore} can deserialize values in parallel.
	 * <br/>
	 * For example:
	 * <pre>
	 * cacheBinaryStore = Map.of(
	 *   BUCKETS, true,
	 *   C_BLOCKS, true
	 * );
	 * </pre>
	 *
	 */
	private Map<StoreMappings, Boolean> cacheBinaryStore = Map.of();

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private transient Validator validator;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	private transient MetricRegistry metricRegistry;

	@JsonIgnore
	private CaffeineSpec getCaffeineSpecParsed() {
		return CaffeineSpec.parse(getCaffeineSpec());
	}

	@Override
	public Collection<NamespaceStorage> discoverNamespaceStorages() {
		return loadNamespacedStores("dataset_", (storePath) -> new NamespaceStorage(this, storePath), NAMESPACE_STORES);
	}

	private <T extends NamespacedStorageImpl> List<T> loadNamespacedStores(String prefix, Function<String, T> creator, Set<String> storesToTest) {
		final File baseDir = getDirectory().toFile();

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}

		final List<T> storages = new ArrayList<>();

		for (File directory : Objects.requireNonNull(baseDir.listFiles((file, name) -> file.isDirectory() && name.startsWith(prefix)))) {

			final String name = directory.getName();

			ConqueryMDC.setLocation(directory.toString());

			try (Environment environment = registry.findOrCreateEnvironment(directory, xodus)) {
				if (!environmentHasStores(environment, storesToTest)) {
					log.warn("No valid {}storage found in {}", prefix, directory);
					continue;
				}
			}

			final T namespacedStorage = creator.apply(name);

			storages.add(namespacedStorage);
		}

		return storages;
	}

	private boolean environmentHasStores(Environment env, Set<String> storesToTest) {
		return env.computeInTransaction(t -> {
			final List<String> allStoreNames = env.getAllStoreNames(t);
			final boolean complete = new HashSet<>(allStoreNames).containsAll(storesToTest);
			if (complete) {
				log.trace("Storage contained all stores: {}", storesToTest);
				return true;
			}

			final HashSet<String> missing = Sets.newHashSet(storesToTest);
			allStoreNames.forEach(missing::remove);
			log.warn("Environment did not contain all required stores. It is missing: {}. It had {}. {}", missing, allStoreNames,
					 loadEnvironmentWithMissingStores
					 ? "Loading environment anyway."
					 : "Skipping environment."
			);

			return loadEnvironmentWithMissingStores;
		});
	}

	@Override
	public Collection<? extends WorkerStorage> discoverWorkerStorages() {
		return loadNamespacedStores("worker_", (storePath) -> new WorkerStorageImpl(this, storePath), WORKER_STORES);
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, DATASET, objectMapper, metricRegistry));
	}

	public <KEY, VALUE> Store<KEY, VALUE> createStore(Environment environment, Validator validator, StoreMappings storeId, ObjectMapper objectMapper,
													  MetricRegistry metricRegistry) {
		final StoreInfo<KEY, VALUE> storeInfo = storeId.storeInfo();
		synchronized (openStoresInEnv) {

			if (openStoresInEnv.get(environment).stream().map(XodusStore::getName).anyMatch(name -> storeInfo.getName().equals(name))) {
				throw new IllegalStateException("Attempted to open an already opened store:" + storeInfo.getName());
			}

			final XodusStore xodusStoretore = new XodusStore(environment, storeInfo.getName(), this::closeStore, this::removeStore);
			openStoresInEnv.put(environment, xodusStoretore);

			boolean cacheBinary = cacheBinaryStore.getOrDefault(storeId, Boolean.FALSE);

			CaffeineSpec binaryCacheSpec = CaffeineSpec.parse("");
			Store<ByteIterable, ByteIterable> binaryStore = cacheBinary ? new CachedStore<>(xodusStoretore, binaryCacheSpec, metricRegistry) : xodusStoretore;

			if (cacheBinary) {
				// Start caching of binary data in the background
				Thread loadStoreThread = new Thread(binaryStore::loadData, "Cache binary store %s from %s".formatted(storeId, environment.getLocation()));
				loadStoreThread.setDaemon(true);
				loadStoreThread.start();
			}


			return new CachedStore<>(
					new SerializingStore<>(
							binaryStore,
							validator,
							objectMapper,
							storeInfo.getKeyType(),
							storeInfo.getValueType(),
							isValidateOnWrite(),
							isRemoveUnreadableFromStore(),
							getUnreadableDataDumpDirectory(),
							getReaderExecutorService()
					),
					getCaffeineSpecParsed(),
					metricRegistry
			);
		}
	}

	private Environment findEnvironment(String pathName) {
		final File path = getStorageDir(pathName);
		return registry.findOrCreateEnvironment(path, getXodus());
	}

	private void closeStore(XodusStore store) {
		final Environment env = store.getEnvironment();
		synchronized (openStoresInEnv) {
			final Collection<XodusStore> stores = openStoresInEnv.get(env);
			stores.remove(store);
			log.info("Closed XodusStore: {}", store);

			if (!stores.isEmpty()) {
				return;
			}
		}
		log.info("Closed last XodusStore in Environment. Closing Environment as well: {}", env.getLocation());

		env.close();
	}

	private void removeStore(XodusStore store) {
		final Environment env = store.getEnvironment();
		synchronized (openStoresInEnv) {
			final Collection<XodusStore> stores = openStoresInEnv.get(env);

			stores.remove(store);

			if (!stores.isEmpty()) {
				return;
			}
		}

		removeEnvironment(env);
	}

	public ExecutorService getReaderExecutorService() {
		if (readerExecutorService == null) {
			readerExecutorService = new ThreadPoolExecutor(
					1, getReaderWorkers(),
					5, TimeUnit.MINUTES,
					new ArrayBlockingQueue<>(getReaderWorkers() * getBufferPerWorker()),
					new ThreadPoolExecutor.CallerRunsPolicy()
			);
		}

		return readerExecutorService;
	}

	/**
	 * Returns this.directory if the list is empty.
	 */
	@NonNull
	@JsonIgnore
	private File getStorageDir(String pathName) {
		return getDirectory().resolve(pathName).toFile();
	}

	private void removeEnvironment(Environment env) {
		log.info("Removed last XodusStore in Environment. Removing Environment as well: {}", env.getLocation());

		final List<String> xodusStore = env.computeInReadonlyTransaction(env::getAllStoreNames);

		if (!xodusStore.isEmpty()) {
			throw new IllegalStateException("Cannot delete environment, because it still contains these stores:" + xodusStore);
		}

		env.close();

		try {
			FileUtil.deleteRecursive(Path.of(env.getLocation()));
		}
		catch (IOException e) {
			log.error("Cannot delete directory of removed Environment[{}]", env.getLocation(), e);
		}
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Table> createTableStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, TABLES, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Import> createImportStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS, objectMapper, metricRegistry));
	}

	@Override
	public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER, objectMapper, metricRegistry));
	}

	@Override
	public SingletonStore<EntityIdMap> createIdMappingStore(String pathName, ObjectMapper objectMapper) {
		final Environment environment = findEnvironment(pathName);

		synchronized (openStoresInEnv) {
			final BigStore<Boolean, EntityIdMap> bigStore =
					new BigStore<>(this, validator, environment, ID_MAPPING.storeInfo(), this::closeStore, this::removeStore, objectMapper, getReaderExecutorService());

			openStoresInEnv.put(bigStore.getDataXodusStore().getEnvironment(), bigStore.getDataXodusStore());
			openStoresInEnv.put(bigStore.getMetaXodusStore().getEnvironment(), bigStore.getMetaXodusStore());
			return new SingletonStore<>(new CachedStore<>(bigStore, getCaffeineSpecParsed(), getMetricRegistry()));
		}
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS, objectMapper, metricRegistry));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<ManagedExecution> createExecutionsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "executions")), validator, EXECUTIONS, objectMapper, metricRegistry));
	}

	private Environment findEnvironment(File path) {
		return registry.findOrCreateEnvironment(path, getXodus());
	}

	private File resolveSubDir(String... subdirs) {
		Path current = getDirectory();

		for (String dir : subdirs) {
			current = current.resolve(dir);
		}

		return current.toFile();
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "formConfigs")), validator, FORM_CONFIG, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<User> createUserStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "users")), validator, AUTH_USER, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "roles")), validator, AUTH_ROLE, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<Group> createGroupStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "groups")), validator, AUTH_GROUP, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, INTERN_TO_EXTERN, objectMapper, metricRegistry));
	}

	@Override
	public IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SEARCH_INDEX, objectMapper, metricRegistry));
	}

	@Override
	public SingletonStore<PreviewConfig> createPreviewStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, ENTITY_PREVIEW, objectMapper, metricRegistry));
	}

	@Override
	public Store<String, Integer> createEntity2BucketStore(String pathName, ObjectMapper objectMapper) {
		return createStore(findEnvironment(pathName), validator, ENTITY_TO_BUCKET, objectMapper, metricRegistry);
	}
}
