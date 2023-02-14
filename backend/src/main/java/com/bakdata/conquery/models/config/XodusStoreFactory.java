package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.storage.StoreMappings.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
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
import com.bakdata.conquery.models.datasets.PreviewConfig;
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
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.DatasetRegistry;
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
			DICTIONARIES.storeInfo().getName() + BigStore.META,
			DICTIONARIES.storeInfo().getName() + BigStore.DATA,
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
					PRIMARY_DICTIONARY.storeInfo().getName(),
					ENTITY_PREVIEW.storeInfo().getName()
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

	private boolean useWeakDictionaryCaching = false;
	@NotNull
	private Duration weakCacheDuration = Duration.hours(48);

	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadableFromStore = false;

	/**
	 * When set, all values that could not be deserialized from the persistent store, are dump into individual files.
	 */
	@Nullable
	private File unreadableDataDumpDirectory = null;

	/**
	 * If set, an environment will not be loaded if it misses a required store.
	 * If not set, the environment is loaded and the application needs to create the store.
	 * This is useful if a new version introduces a new store, but will also alter the environment upon reading.
	 */
	private boolean loadEnvironmentWithMissingStores = false;

	@JsonIgnore
	private transient Validator validator;

	@JsonIgnore
	private final BiMap<File, Environment> activeEnvironments = HashBiMap.create();

	@JsonIgnore
	private final transient Multimap<Environment, XodusStore>
			openStoresInEnv =
			Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());

	@Override
	public Collection<NamespaceStorage> discoverNamespaceStorages() {
		return loadNamespacedStores("dataset_", (storePath) -> new NamespaceStorage(this, storePath, getValidator()), NAMESPACE_STORES);
	}

	@Override
	public Collection<WorkerStorage> discoverWorkerStorages() {
		return loadNamespacedStores("worker_", (storePath) -> new WorkerStorage(this, validator, storePath), WORKER_STORES);
	}


	private <T extends NamespacedStorage> List<T> loadNamespacedStores(String prefix, Function<String, T> creator, Set<String> storesToTest) {
		File baseDir = getDirectory().toFile();

		if (baseDir.mkdirs()) {
			log.warn("Had to create Storage Dir at `{}`", baseDir);
		}

		List<T> storages = new ArrayList<>();

		for (File directory : Objects.requireNonNull(baseDir.listFiles((file, name) -> file.isDirectory() && name.startsWith(prefix)))) {

			final String name = directory.getName();

			ConqueryMDC.setLocation(directory.toString());

			if (!environmentHasStores(directory, storesToTest)) {
				log.warn("No valid WorkerStorage found in {}", directory);
				continue;
			}

			T namespacedStorage = creator.apply(name);

			storages.add(namespacedStorage);
		}

		return storages;
	}

	private boolean environmentHasStores(File pathName, Set<String> storesToTest) {
		Environment env = findEnvironment(pathName);
		boolean exists = env.computeInTransaction(t -> {
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
		if (!exists) {
			closeEnvironment(env);
		}
		return exists;
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, DATASET, objectMapper));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SECONDARY_IDS, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, INTERN_TO_EXTERN, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, SEARCH_INDEX, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public SingletonStore<PreviewConfig> createPreviewStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, ENTITY_PREVIEW, centralRegistry.injectIntoNew(objectMapper)));
	}

	@Override
	public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, TABLES, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		final Environment environment = findEnvironment(pathName);

		final BigStore<Id<Dictionary>, Dictionary> bigStore;

		synchronized (openStoresInEnv) {
			bigStore =
					new BigStore<>(
							this,
							validator,
							environment,
							DICTIONARIES.storeInfo(),
							this::closeStore,
							this::removeStore,
							centralRegistry.injectIntoNew(objectMapper)
					);
			openStoresInEnv.put(bigStore.getDataXodusStore().getEnvironment(), bigStore.getDataXodusStore());
			openStoresInEnv.put(bigStore.getMetaXodusStore().getEnvironment(), bigStore.getMetaXodusStore());
		}

		if (useWeakDictionaryCaching) {
			return StoreMappings.identifiableCachedStore(new WeakCachedStore<>(bigStore, getWeakCacheDuration()), centralRegistry);
		}
		return StoreMappings.identifiable(StoreMappings.cached(bigStore), centralRegistry);
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, CONCEPTS, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, IMPORTS, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, C_BLOCKS, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(pathName), validator, BUCKETS, centralRegistry.injectIntoNew(objectMapper)), centralRegistry);
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
					new BigStore<>(this, validator, environment, ID_MAPPING.storeInfo(), this::closeStore, this::removeStore, objectMapper);

			openStoresInEnv.put(bigStore.getDataXodusStore().getEnvironment(), bigStore.getDataXodusStore());
			openStoresInEnv.put(bigStore.getMetaXodusStore().getEnvironment(), bigStore.getMetaXodusStore());
			return new SingletonStore<>(new CachedStore<>(bigStore));
		}
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, WORKER_TO_BUCKETS, objectMapper));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, STRUCTURE, centralRegistry.injectIntoNew(objectMapper)));
	}

	@Override
	public IdentifiableStore<ManagedExecution> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "executions")), validator, EXECUTIONS, datasetRegistry.injectInto(centralRegistry.injectIntoNew(objectMapper))), centralRegistry);
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "formConfigs")), validator, FORM_CONFIG, datasetRegistry.injectInto(centralRegistry.injectIntoNew(objectMapper))), centralRegistry);
	}

	@Override
	public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "users")), validator, AUTH_USER, objectMapper), centralRegistry);
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "roles")), validator, AUTH_ROLE, objectMapper), centralRegistry);
	}


	@Override
	public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(createStore(findEnvironment(resolveSubDir(pathName, "groups")), validator, AUTH_GROUP, objectMapper), centralRegistry);
	}

	@Override
	public SingletonStore<Dictionary> createPrimaryDictionaryStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.singleton(createStore(findEnvironment(pathName), validator, PRIMARY_DICTIONARY, centralRegistry.injectIntoNew(objectMapper)));
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
			try {
				return activeEnvironments.computeIfAbsent(path, (p) -> Environments.newInstance(path, getXodus().createConfig()));
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to open environment: " + path, e);
			}
		}
	}

	private Environment findEnvironment(String pathName) {
		File path = getStorageDir(pathName);
		return findEnvironment(path);
	}

	private void closeStore(XodusStore store) {
		Environment env = store.getEnvironment();
		synchronized (openStoresInEnv) {
			Collection<XodusStore> stores = openStoresInEnv.get(env);
			stores.remove(store);
			log.info("Closed XodusStore: {}", store);

			if (!stores.isEmpty()) {
				return;
			}
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

	private void removeStore(XodusStore store) {
		Environment env = store.getEnvironment();
		synchronized (openStoresInEnv){
			Collection<XodusStore> stores = openStoresInEnv.get(env);

			stores.remove(store);

			if (!stores.isEmpty()) {
				return;
			}
		}

		removeEnvironment(env);
	}

	private void removeEnvironment(Environment env) {
		log.info("Removed last XodusStore in Environment. Removing Environment as well: {}", env.getLocation());

		final List<String> xodusStore= env.computeInReadonlyTransaction(env::getAllStoreNames);

		if (!xodusStore.isEmpty()){
			throw new IllegalStateException("Cannot delete environment, because it still contains these stores:" + xodusStore);
		}

		closeEnvironment(env);

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

			if(openStoresInEnv.get(environment).stream().map(XodusStore::getName).anyMatch(name -> storeInfo.getName().equals(name))){
				throw new IllegalStateException("Attempted to open an already opened store:" + storeInfo.getName());
			}
			final XodusStore store =
					new XodusStore(environment, storeInfo.getName(), this::closeStore, this::removeStore);
			openStoresInEnv.put(environment, store);
			return new CachedStore<>(
					new SerializingStore<>(
							store,
							validator,
							objectMapper,
							storeInfo.getKeyType(),
							storeInfo.getValueType(),
							this.isValidateOnWrite(),
							this.isRemoveUnreadableFromStore(),
							this.getUnreadableDataDumpDirectory()
					));
		}
	}

}
