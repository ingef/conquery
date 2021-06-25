package com.bakdata.conquery.util;

import static com.bakdata.conquery.io.storage.StoreInfo.*;

import java.util.Collection;
import java.util.Collections;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.IdentifiableStore;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.config.StoreFactory;
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

@CPSType(id = "NON_PERSISTENT", base = StoreFactory.class)
public class NonPersistentStoreFactory implements StoreFactory {

    @Override
    public Collection<NamespaceStorage> loadNamespaceStorages() {

        return Collections.emptyList();
    }

    @Override
    public Collection<WorkerStorage> loadWorkerStorages() {

        return Collections.emptyList();
    }

    @Override
    public SingletonStore<Dataset> createDatasetStore(String pathName) {
        return DATASET.singleton(new NonPersistentStore());
    }

    @Override
    public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, String pathName) {
        return SECONDARY_IDS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, String pathName) {
        return TABLES.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, String pathName) {
        return DICTIONARIES.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, String pathName) {
        return CONCEPTS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, String pathName) {
        return IMPORTS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, String pathName) {
        return C_BLOCKS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, String pathName) {
        return BUCKETS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public SingletonStore<WorkerInformation> createWorkerInformationStore(String pathName) {
        return WORKER.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<PersistentIdMap> createIdMappingStore(String pathName) {
        return ID_MAPPING.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(String pathName) {
        return WORKER_TO_BUCKETS.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<StructureNode[]> createStructureStore(String pathName, SingletonNamespaceCollection centralRegistry) {
        return STRUCTURE.singleton(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<ManagedExecution<?>> createExecutionsStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName) {
        return EXECUTIONS.identifiable(new NonPersistentStore(), centralRegistry, datasetRegistry);
    }

    @Override
    public IdentifiableStore<FormConfig> createFormConfigStore(CentralRegistry centralRegistry, DatasetRegistry datasetRegistry, String pathName) {
        return FORM_CONFIG.identifiable(new NonPersistentStore(), centralRegistry, datasetRegistry);
    }

    @Override
    public IdentifiableStore<User> createUserStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_USER.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Role> createRoleStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_ROLE.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Group> createGroupStore(CentralRegistry centralRegistry, String pathName) {
        return AUTH_GROUP.identifiable(new NonPersistentStore(), centralRegistry);
    }

    /**
     * @implNote intended for Unit-tests
     */
    public MetaStorage createMetaStorage(){
        return new MetaStorage(null, this,  null);
    }
}
