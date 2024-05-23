package com.bakdata.conquery.models.config;

import java.util.Collection;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.CachedStore;
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
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
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

	IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Table> createTableStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Concept<?>> createConceptStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Import> createImportStore(String pathName, ObjectMapper objectMapper);

	// WorkerStorage
	IdentifiableStore<CBlock> createCBlockStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Bucket> createBucketStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName, ObjectMapper objectMapper);

	// NamespaceStorage
	SingletonStore<EntityIdMap> createIdMappingStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<StructureNode[]> createStructureStore(String pathName, ObjectMapper objectMapper);

	// MetaStorage
	IdentifiableStore<ManagedExecution> createExecutionsStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<FormConfig> createFormConfigStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<User> createUserStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Role> createRoleStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<Group> createGroupStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<InternToExternMapper> createInternToExternMappingStore(String pathName, ObjectMapper objectMapper);

	IdentifiableStore<SearchIndex> createSearchIndexStore(String pathName, ObjectMapper objectMapper);

	SingletonStore<PreviewConfig> createPreviewStore(String pathName, ObjectMapper objectMapper);

	CachedStore<String, Integer> createEntity2BucketStore(String pathName, ObjectMapper objectMapper);
}
