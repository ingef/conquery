package com.bakdata.conquery.util;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Validator;

public class NonPersistentNamespaceStorage extends NonPersistentNamespacedCentralRegisteredStorage implements NamespaceStorage {

    @Getter @Setter
    private MetaStorage metaStorage;
    private StructureNode[] structureNodes;
    private PersistentIdMap persistentIdMap;
    private WorkerToBucketsMap workerToBucketsMap;

    public NonPersistentNamespaceStorage(Validator validator) {
        super(validator);
    }

    @Override
    public StructureNode[] getStructure() {
        return structureNodes;
    }

    @Override
    public void updateStructure(StructureNode[] structure) throws JSONException {
        this.structureNodes = structure;
    }

    @Override
    public PersistentIdMap getIdMapping() {
        return persistentIdMap;
    }

    @Override
    public void updateIdMapping(PersistentIdMap idMap) throws JSONException {
        this.persistentIdMap = idMap;
    }

    @Override
    public void setWorkerToBucketsMap(WorkerToBucketsMap map) {
        this.workerToBucketsMap = map;
    }

    @Override
    public WorkerToBucketsMap getWorkerBuckets() {
        return workerToBucketsMap;
    }
}
