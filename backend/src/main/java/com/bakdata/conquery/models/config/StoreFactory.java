package com.bakdata.conquery.models.config;

import java.util.Collection;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
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
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StoreFactory {

	default void init(ManagerNode managerNode) {};
	default void init(ShardNode shardNode) {};

	Collection<NamespaceStorage> loadNamespaceStorages();

	Collection<WorkerStorage> loadWorkerStorages();

	// NamespacedStorage (Important for serdes communication between manager and shards)
	SingletonStore<Dataset> createDatasetStore(String pathName);
	IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName);

	// WorkerStorage
	IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName);
	SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName);

	// NamespaceStorage
	SingletonStore<PersistentIdMap> createIdMappingStore(String pathName);
	SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName);
	SingletonStore<StructureNode[]> createStructureStore(String pathName, SingletonNamespaceCollection centralRegistry);

	// MetaStorage
    IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName);
	IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName);
	IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName);
	IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName);
}
