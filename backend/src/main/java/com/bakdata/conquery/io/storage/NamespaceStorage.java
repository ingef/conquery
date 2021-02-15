package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class NamespaceStorage extends NamespacedStorage {

    @Getter
    @Setter
    @NonNull
    private MetaStorage metaStorage;
    protected SingletonStore<PersistentIdMap> idMapping;
    protected SingletonStore<StructureNode[]> structure;
    protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

    @Getter
    private final boolean registerImports = true;

    public NamespaceStorage(Validator validator, StoreFactory storageFactory, List<String> pathName) {
        super(validator, storageFactory, pathName);

        idMapping = storageFactory.createIdMappingStore(pathName);
        structure = storageFactory.createStructureStore(pathName);
        workerToBuckets = storageFactory.createWorkerToBucketsStore(pathName);
    }

    @Override
    public void loadData() {
        super.loadData();
        idMapping.loadData();
        structure.loadData();
        workerToBuckets.loadData();
    }

    @Override
    public void clear() {
        super.clear();
        idMapping.clear();
        structure.clear();
        workerToBuckets.clear();
    }

    @Override
    public void remove() {
        super.remove();
        idMapping.remove();
        structure.remove();
        workerToBuckets.remove();


    }

    @Override
    public void close() throws IOException {
        super.close();
        idMapping.close();
        structure.close();
        workerToBuckets.close();
    }

    public PersistentIdMap getIdMapping() {
        return idMapping.get();
    }


    public void updateIdMapping(PersistentIdMap idMapping) throws JSONException {
        this.idMapping.update(idMapping);
    }

    public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
        workerToBuckets.update(map);
    }

    public WorkerToBucketsMap getWorkerBuckets() {
        return workerToBuckets.get();
    }


    public StructureNode[] getStructure() {
        return Objects.requireNonNullElseGet(structure.get(), () -> new StructureNode[0]);
    }

    public void updateStructure(StructureNode[] structure) throws JSONException {
        this.structure.update(structure);
    }
}
