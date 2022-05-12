package com.bakdata.conquery.util;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.StoreMappings;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.StoreFactory;
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
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@CPSType(id = "NON_PERSISTENT", base = StoreFactory.class)
public class NonPersistentStoreFactory implements StoreFactory {

	private final Map<String, NonPersistentStore<Boolean, Dataset>> datasetStores = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<SecondaryIdDescription>, SecondaryIdDescription>> secondaryIdDescriptionStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Table>, Table>> tableStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Dictionary>, Dictionary>> dictionaryStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Concept<?>>, Concept<?>>> conceptStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Import>, Import>> importStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<CBlock>, CBlock>> cBlockStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Bucket>, Bucket>> bucketStore = new ConcurrentHashMap<>();

	private final Map<String, NonPersistentStore<Boolean, WorkerInformation>> workerStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, EntityIdMap>> idMappingStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, WorkerToBucketsMap>> workerToBucketStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, StructureNode[]>> structureStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<ManagedExecution<?>>, ManagedExecution<?>>> executionStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<FormConfig>, FormConfig>> formConfigStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<User>, User>> userStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Role>, Role>> roleStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<IId<Group>, Group>> groupStore = new ConcurrentHashMap<>();
	private final Map<String, NonPersistentStore<Boolean, Dictionary>> primaryDictionaryStoreStore = new ConcurrentHashMap<>();


	@Override
	public Collection<NamespaceStorage> discoverNamespaceStorages() {
		return Collections.emptyList();
	}

	@Override
	public Collection<WorkerStorage> discoverWorkerStorages() {
		return Collections.emptyList();
	}

	@Override
	public SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper) {
		return StoreMappings.singleton(datasetStores.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(secondaryIdDescriptionStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(tableStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(dictionaryStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(conceptStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(importStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(cBlockStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(bucketStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
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
	public SingletonStore<StructureNode[]> createStructureStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.singleton(structureStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	@Override
	public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(executionStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(formConfigStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(userStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(roleStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper) {
		return StoreMappings.identifiable(groupStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()), centralRegistry);
	}

	@Override
	public SingletonStore<Dictionary> createPrimaryDictionaryStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper) {
		return StoreMappings.singleton(primaryDictionaryStoreStore.computeIfAbsent(pathName, n -> new NonPersistentStore<>()));
	}

	/**
	 * @implNote intended for Unit-tests
	 */
	public MetaStorage createMetaStorage() {
		final MetaStorage metaStorage = new MetaStorage(this, null);
		metaStorage.openStores(null);
		return metaStorage;
	}
}
