package com.bakdata.conquery.util;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.Getter;

import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NonPersistentWorkerStorage extends NonPersistentNamespacedCentralRegisteredStorage implements WorkerStorage {

    private IdMap<BucketId, Bucket> buckets = new IdMap<>();
    private IdMap<CBlockId, CBlock> cBlocks = new IdMap<>();
    private WorkerInformation workerInformation;

    public NonPersistentWorkerStorage(Validator validator) {
        super(validator);
    }

    @Override
    public String getStorageOrigin() {
        return null;
    }

    @Override
    public WorkerInformation getWorker() {
        return workerInformation;
    }

    @Override
    public void setWorker(WorkerInformation worker) {
        this.workerInformation = worker;
    }

    @Override
    public void updateWorker(WorkerInformation worker) {
        this.workerInformation = worker;
    }

    @Override
    public void addBucket(Bucket bucket) {
        buckets.add(bucket);
    }

    @Override
    public Bucket getBucket(BucketId id) {
        return buckets.get(id);
    }

    @Override
    public void removeBucket(BucketId id) {
        buckets.remove(id);
    }

    @Override
    public Collection<Bucket> getAllBuckets() {
        return buckets.values();
    }

    @Override
    public void addCBlock(CBlock cBlock) {
        cBlocks.add(cBlock);
    }

    @Override
    public CBlock getCBlock(CBlockId id) {
        return cBlocks.get(id);
    }

    @Override
    public void updateCBlock(CBlock cBlock) {
        cBlocks.update(cBlock);
    }

    @Override
    public void removeCBlock(CBlockId id) {
        cBlocks.remove(id);
    }

    @Override
    public Collection<CBlock> getAllCBlocks() {
        return cBlocks.values();
    }
}
