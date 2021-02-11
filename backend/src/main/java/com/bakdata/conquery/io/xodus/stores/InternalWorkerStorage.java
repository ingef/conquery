package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.config.StorageFactory;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validator;
import java.util.Collection;
import java.util.List;

@Slf4j
public class InternalWorkerStorage extends InternalNamespacedStorage implements WorkerStorage {

    private SingletonStore<WorkerInformation> worker;
    private IdentifiableStore<Bucket> bluckets;
    private IdentifiableStore<CBlock> cBlocks;

    public InternalWorkerStorage(Validator validator, StorageFactory storageFactory, List<String> pathName) {
        super(validator, storageFactory, pathName);

        worker = storageFactory.createWorkerInformationStore(pathName);
        bluckets = storageFactory.createBucketStore(centralRegistry, pathName);
        cBlocks = storageFactory.createCBlockStore(centralRegistry, pathName);

        decorateBucketStore(bluckets);
        decorateCBlockStore(cBlocks);
    }


    @Override
    public void addCBlock(CBlock cBlock) {
        log.debug("Adding CBlock[{}]", cBlock.getId());
        cBlocks.add(cBlock);
    }

    @Override
    public CBlock getCBlock(CBlockId id) {
        return cBlocks.get(id);
    }

    // TODO method is unused, delete it.
    @Override
    public void updateCBlock(CBlock cBlock) {
        cBlocks.update(cBlock);
    }

    @Override
    public void removeCBlock(CBlockId id) {
        log.debug("Removing CBlock[{}]", id);
        cBlocks.remove(id);
    }

    @Override
    public void addDictionary(Dictionary dict) {
        if (dict.getId().equals(ConqueryConstants.getPrimaryDictionary(getDataset()))) {
            throw new IllegalStateException("Workers may not receive the primary dictionary");
        }

        super.addDictionary(dict);
    }

    @Override
    public Collection<CBlock> getAllCBlocks() {
        return cBlocks.getAll();
    }

    @Override
    public void addBucket(Bucket bucket) {
        log.debug("Adding Bucket[{}]", bucket.getId());
        bluckets.add(bucket);
    }

    @Override
    public Bucket getBucket(BucketId id) {
        return bluckets.get(id);
    }

    @Override
    public void removeBucket(BucketId id) {
        log.debug("Removing Bucket[{}]", id);
        bluckets.remove(id);
    }

    @Override
    public Collection<Bucket> getAllBuckets() {
        return bluckets.getAll();
    }

    @Override
    public WorkerInformation getWorker() {
        return worker.get();
    }

    //TODO remove duplication
    @Override
    public void setWorker(WorkerInformation worker) {
        this.worker.add(worker);
    }

    @Override
    public void updateWorker(WorkerInformation worker) {
        this.worker.update(worker);
    }

    //block manager overrides
    @Override
    public void updateConcept(Concept<?> concept) {
        log.debug("Updating Concept[{}]", concept.getId());
        concepts.update(concept);
    }

    @Override
    public void removeConcept(ConceptId id) {
        log.debug("Removing Concept[{}]", id);
        concepts.remove(id);
    }
}
