package com.bakdata.conquery.models.config;

import java.util.Collection;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
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
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@CPSBase
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public interface StoreFactory {

	Collection<NamespaceStorage> discoverNamespaceStorages();

	Collection<WorkerStorage> discoverWorkerStorages();

	// NamespacedStorage (Important for serdes communication between manager and shards)
	SingletonStore<Dataset> createDatasetStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	// WorkerStorage
	IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName, ObjectMapper objectMapper);

	SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName, ObjectMapper objectMapper);

	// NamespaceStorage
	SingletonStore<EntityIdMap> createIdMappingStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<StructureNode[]> createStructureStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper);

	// MetaStorage
	IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName, ObjectMapper objectMapper);

	IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper);

	IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper);

	IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName, MetaStorage storage, ObjectMapper objectMapper);

	SingletonStore<Dictionary> createPrimaryDictionaryStore(String pathName, CentralRegistry namespaceCollection, ObjectMapper objectMapper);

	IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper);

	IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper);

	SingletonStore<PreviewConfig> createPreviewStore(String pathName, CentralRegistry centralRegistry, ObjectMapper objectMapper);
}
