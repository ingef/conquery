package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreMappings.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
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
import com.bakdata.conquery.io.storage.*;
import com.bakdata.conquery.io.storage.xodus.stores.*;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.*;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import io.dropwizard.validation.ValidationMethod;
import jetbrains.exodus.env.Environment;
import lombok.*;
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
	 * See <a href="https://github.com/ben-manes/caffeine/wiki/Specification">CaffeinSpec</a>
	 */
	@NotEmpty
	private String caffeineSpec = "maximumSize=0";

	@JsonIgnore
	private transient Validator validator;

	@JsonIgnore
	private final transient Multimap<Environment, XodusStore>
			openStoresInEnv =
			Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

	@JsonIgnore
	@ValidationMethod(message = "CaffeineSpec cannot be parsed")
	public boolean isValidCaffeineSpec() {
		try {
			CaffeineSpec.parse(caffeineSpec);
			return true;
		}
		catch (Exception e) {
			log.error("Unable to parse caffeine spec", e);
			return false;
		}
	}

	@Override
	public Collection<NamespaceStorage> discoverNamespaceStorages() {
		return loadNamespacedStores("dataset_", (storePath) -> new NamespaceStorage(this, storePath), NAMESPACE_STORES);
	}

	@Override
	public Collection<WorkerStorage> discoverWorkerStorages() {
		return loadNamespacedStores("worker_", (storePath) -> new WorkerStorage(this, storePath), WORKER_STORES);
	}


	private <T extends NamespacedStorage> List<T> loadNamespacedStores(String prefix, Function<String, T> creator, Set<String> storesToTest) {
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
	public SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, DATASET, objectMapper));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS, objectMapper));
	}

	@Override
	public IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, INTERN_TO_EXTERN, objectMapper));
	}

	@Override
	public IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SEARCH_INDEX, objectMapper));
	}

	@Override
	public SingletonStore<PreviewConfig> createPreviewStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, ENTITY_PREVIEW, objectMapper));
	}

	@Override
	public Store<String, Integer> createEntity2BucketStore(String pathName, ObjectMapper objectMapper) {
		return createStore(findEnvironment(pathName), validator, ENTITY_TO_BUCKET, objectMapper);
	}

	@Override
	public IdentifiableStore<Table> createTableStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, TABLES, objectMapper));
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS, objectMapper));
	}

	@Override
	public IdentifiableStore<Import> createImportStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS, objectMapper));
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS, objectMapper));
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS, objectMapper));
	}

	@Override
	public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER, objectMapper));
	}

	@Override
	public SingletonStore<EntityIdMap> createIdMappingStore(String pathName, ObjectMapper objectMapper) {
		final Environment environment = findEnvironment(pathName);

		synchronized (openStoresInEnv) {
			final BigStore<Boolean, EntityIdMap> bigStore =
					new BigStore<>(this, validator, environment, ID_MAPPING.storeInfo(), this::closeStore, this::removeStore, objectMapper, getReaderExecutorService());

			openStoresInEnv.put(bigStore.getDataXodusStore().getEnvironment(), bigStore.getDataXodusStore());
			openStoresInEnv.put(bigStore.getMetaXodusStore().getEnvironment(), bigStore.getMetaXodusStore());
			return new SingletonStore<>(bigStore);
		}
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS, objectMapper));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE, objectMapper));
	}

	@Override
	public IdentifiableStore<ManagedExecution> createExecutionsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "executions")), validator, EXECUTIONS, objectMapper));
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "formConfigs")), validator, FORM_CONFIG, objectMapper));
	}

	@Override
	public IdentifiableStore<User> createUserStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "users")), validator, AUTH_USER, objectMapper));
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "roles")), validator, AUTH_ROLE, objectMapper));
	}


	@Override
	public IdentifiableStore<Group> createGroupStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "groups")), validator, AUTH_GROUP, objectMapper));
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


	private Environment findEnvironment(String pathName) {
		final File path = getStorageDir(pathName);
		return registry.findOrCreateEnvironment(path, getXodus());
	}

	private Environment findEnvironment(File path) {
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

	public <KEY, VALUE> Store<KEY, VALUE> createStore(Environment environment, Validator validator, StoreMappings storeId, ObjectMapper objectMapper) {
		final StoreInfo<KEY, VALUE> storeInfo = storeId.storeInfo();
		synchronized (openStoresInEnv) {

			if (openStoresInEnv.get(environment).stream().map(XodusStore::getName).anyMatch(name -> storeInfo.getName().equals(name))) {
				throw new IllegalStateException("Attempted to open an already opened store:" + storeInfo.getName());
			}

			final XodusStore store = new XodusStore(environment, storeInfo.getName(), this::closeStore, this::removeStore);

			openStoresInEnv.put(environment, store);

			return new SerializingStore<>(
					store,
					validator,
					objectMapper,
					storeInfo.getKeyType(),
					storeInfo.getValueType(),
					isValidateOnWrite(),
					isRemoveUnreadableFromStore(),
					getUnreadableDataDumpDirectory(),
					getReaderExecutorService()
			);
		}
	}

	@Override
	public CaffeineSpec getCacheSpec() {
		return CaffeineSpec.parse(getCaffeineSpec());
	}
}
