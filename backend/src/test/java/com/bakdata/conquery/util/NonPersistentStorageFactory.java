package com.bakdata.conquery.util;

import static com.bakdata.conquery.io.xodus.StoreInfo.*;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import org.apache.commons.collections4.queue.UnmodifiableQueue;

import javax.validation.Validator;
import java.util.*;

@CPSType(id = "NON_PERSISTENT", base = StorageFactory.class)
public class NonPersistentStorageFactory implements StorageFactory {

    @Override
    public MetaStorage createMetaStorage(Validator validator, List<String> pathName, DatasetRegistry datasets) {
        return new NonPersistentMetaStorage(datasets, validator);
    }

    @Override
    public NamespaceStorage createNamespaceStorage(Validator validator, List<String> pathName) {
        return new NonPersistentNamespaceStorage(validator);
    }

    @Override
    public WorkerStorage createWorkerStorage(Validator validator, List<String> pathName) {
        return new NonPersistentWorkerStorage(validator);
    }

    @Override
    public Collection<NamespaceStorage> loadNamespaceStorages(ManagerNode managerNode, List<String> pathName) {

        return Collections.emptyList();
    }

    @Override
    public Collection<WorkerStorage> loadWorkerStorages(ShardNode shardNode, List<String> pathName) {

        return Collections.emptyList();
    }

    @Override
    public SingletonStore<Dataset> createDatasetStore(List<String> pathName) {
        return DATASET.singleton(new NonPersistentStore());
    }

    @Override
    public IdentifiableStore<SecondaryIdDescription> createSecondaryIdDescriptionStore(CentralRegistry centralRegistry, List<String> pathName) {
        return SECONDARY_IDS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Table> createTableStore(CentralRegistry centralRegistry, List<String> pathName) {
        return TABLES.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Dictionary> createDictionaryStore(CentralRegistry centralRegistry, List<String> pathName) {
        return DICTIONARIES.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Concept<?>> createConceptStore(CentralRegistry centralRegistry, List<String> pathName) {
        return CONCEPTS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Import> createImportStore(CentralRegistry centralRegistry, List<String> pathName) {
        return IMPORTS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<CBlock> createCBlockStore(CentralRegistry centralRegistry, List<String> pathName) {
        return C_BLOCKS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public IdentifiableStore<Bucket> createBucketStore(CentralRegistry centralRegistry, List<String> pathName) {
        return BUCKETS.identifiable(new NonPersistentStore(), centralRegistry);
    }

    @Override
    public SingletonStore<WorkerInformation> createWorkerInformationStore(List<String> pathName) {
        return WORKER.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<PersistentIdMap> createIdMappingStore(List<String> pathName) {
        return ID_MAPPING.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<WorkerToBucketsMap> createWorkerToBucketsStore(List<String> pathName) {
        return WORKER_TO_BUCKETS.singleton(new NonPersistentStore());
    }

    @Override
    public SingletonStore<StructureNode[]> createStructureStore(List<String> pathName) {
        return STRUCTURE.singleton(new NonPersistentStore());
    }
}
