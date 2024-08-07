package com.bakdata.conquery.io.storage;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(of = "worker")
public class WorkerStorage extends NamespacedStorage implements Injectable {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> buckets;
	private IdentifiableStore<CBlock> cBlocks;

	public WorkerStorage(StoreFactory storageFactory, String pathName) {
		super(storageFactory, pathName);
	}

	@Override
	public void openStores(ObjectMapper objectMapper) {
		super.openStores(objectMapper);

		worker = getStorageFactory().createWorkerInformationStore(getPathName(), objectMapper);
		buckets = getStorageFactory().createBucketStore(centralRegistry, getPathName(), objectMapper);
		cBlocks = getStorageFactory().createCBlockStore(centralRegistry, getPathName(), objectMapper);
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(
				dataset,
				secondaryIds,
				tables,
				imports,
				concepts,

				worker,
				buckets,
				cBlocks
		);
	}


	public void addCBlock(CBlock cBlock) {
		log.trace("Adding CBlock[{}]", cBlock.getId());
		cBlocks.add(cBlock);
	}

	public CBlock getCBlock(CBlockId id) {
		return cBlocks.get(id);
	}

	public void removeCBlock(CBlockId id) {
		log.trace("Removing CBlock[{}]", id);
		cBlocks.remove(id);
	}

	public Collection<CBlock> getAllCBlocks() {
		return cBlocks.getAll();
	}

	public void addBucket(Bucket bucket) {
		log.trace("Adding Bucket[{}]", bucket.getId());
		buckets.add(bucket);
	}

	public Bucket getBucket(BucketId id) {
		return buckets.get(id);
	}

	public void removeBucket(BucketId id) {
		log.trace("Removing Bucket[{}]", id);
		buckets.remove(id);
	}

	public Collection<Bucket> getAllBuckets() {
		return buckets.getAll();
	}

	public WorkerInformation getWorker() {
		return worker.get();
	}

	public void setWorker(WorkerInformation worker) {
		this.worker.add(worker);
	}

	public void updateWorker(WorkerInformation worker) {
		this.worker.update(worker);
	}

	//block manager overrides
	public void updateConcept(Concept<?> concept) {
		log.debug("Updating Concept[{}]", concept.getId());
		concepts.update(concept);
	}

	public void removeConcept(ConceptId id) {
		log.debug("Removing Concept[{}]", id);
		concepts.remove(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return super.inject(values).add(WorkerStorage.class, this);
	}
}
