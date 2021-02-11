package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.Validator;
import java.util.List;
import java.util.Objects;

public class InternalNamespaceStorage extends InternalNamespacedStorage implements NamespaceStorage {

    @Getter
    @Setter
    @NonNull
    private MetaStorage metaStorage;
    protected SingletonStore<PersistentIdMap> idMapping;
    protected SingletonStore<StructureNode[]> structure;
    protected SingletonStore<WorkerToBucketsMap> workerToBuckets;

    public InternalNamespaceStorage(Validator validator, StorageFactory storageFactory, List<String> pathName) {
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

    }

    @Override
    public PersistentIdMap getIdMapping() {
        return idMapping.get();
    }


    @Override
    public void updateIdMapping(PersistentIdMap idMapping) throws JSONException {
        this.idMapping.update(idMapping);
    }

    @Override
    public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
        workerToBuckets.update(map);
    }

    public WorkerToBucketsMap getWorkerBuckets() {
        return workerToBuckets.get();
    }



    @Override
    public StructureNode[] getStructure() {
        return Objects.requireNonNullElseGet(structure.get(), ()->new StructureNode[0]);
    }

    @Override
    public void updateStructure(StructureNode[] structure) throws JSONException {
        this.structure.update(structure);
    }
}
