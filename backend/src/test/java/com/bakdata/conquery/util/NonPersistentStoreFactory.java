package com.bakdata.conquery.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StoreFactory;
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
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@CPSType(id = "NON_PERSISTENT", base = StoreFactory.class)
public class NonPersistentStoreFactory implements StoreFactory {

	private final Map<String, NonPersistentStore<Boolean, Dataset>> datasetStores = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<SecondaryIdDescription>, SecondaryIdDescription>> secondaryIdDescriptionStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Table>, Table>> tableStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Concept<?>>, Concept<?>>> conceptStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Import>, Import>> importStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<CBlock>, CBlock>> cBlockStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Bucket>, Bucket>> bucketStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<InternToExternMapper>, InternToExternMapper>> internToExternStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<SearchIndex>, SearchIndex>> searchIndexStore = new ConcurrentHashMap<>();

	private final Map<String, NonPersistentStore<Boolean, WorkerInformation>> workerStore = new ConcurrentHashMap<>();

	private final Map<String, NonPersistentStore<Boolean, PreviewConfig>> previewStore = new ConcurrentHashMap<>();

	private final Map<String, NonPersistentStore<Boolean, EntityIdMap>> idMappingStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, WorkerToBucketsMap>> workerToBucketStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, StructureNode[]>> structureStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<ManagedExecution>, ManagedExecution>> executionStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<FormConfig>, FormConfig>> formConfigStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<User>, User>> userStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Role>, Role>> roleStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Id<Group>, Group>> groupStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<String, Integer>> entity2Bucket = new ConcurrentHashMap<>();


	@Override
	public boolean isLoadStoresOnStart() {
		return false;
	}

	@Override
	public Collection<NamespaceStorage> discoverNamespaceStorages() {
		return Collections.emptyList();
	}

	@Override
	public Collection<? extends WorkerStorage> discoverWorkerStorages() {
		return Collections.emptyList();
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(datasetStores.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(secondaryIdDescriptionStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Table> createTableStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(tableStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(conceptStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Import> createImportStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(importStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(cBlockStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(bucketStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(workerStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public SingletonStore<EntityIdMap> createIdMappingStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(idMappingStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(workerToBucketStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(structureStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<ManagedExecution> createExecutionsStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(executionStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(formConfigStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<User> createUserStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(userStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(roleStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<Group> createGroupStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(groupStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(internToExternStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(searchIndexStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public SingletonStore<PreviewConfig> createPreviewStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(previewStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));

	}

	@Override
	public Store<String, Integer> createEntity2BucketStore(String pathName, ObjectMapper objectMapper) {
		return entity2Bucket.computeIfAbsent(pathName, ignored -> new NonPersistentStore<>());
	}

	/**
	 * @implNote intended for Unit-tests
	 */
	public MetaStorage createMetaStorage() {
		final MetaStorage metaStorage = new MetaStorage(this);
		metaStorage.openStores(null);
		return metaStorage;
	}

	/**
	 * @implNote intended for Unit-tests
	 */
	public NamespaceStorage createNamespaceStorage() {
		final NamespaceStorage storage = new NamespaceStorage(this, "_");
		storage.openStores(null);
		return storage;
	}
}
