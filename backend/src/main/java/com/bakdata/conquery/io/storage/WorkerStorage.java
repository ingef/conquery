package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(of = "worker")
public class WorkerStorage extends NamespacedStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> buckets;
	private IdentifiableStore<CBlock> cBlocks;

	private WorkerInformation cachedWorker;

	public WorkerStorage(StoreFactory storageFactory, Validator validator, String pathName) {
		super(storageFactory, pathName);
	}

	@Override
	public void openStores(ObjectMapper objectMapper, MetricRegistry metricRegistry) {
		super.openStores(objectMapper, metricRegistry);

		worker = getStorageFactory().createWorkerInformationStore(getPathName(), objectMapper);
		buckets = getStorageFactory().createBucketStore(getPathName(), objectMapper);
		cBlocks = getStorageFactory().createCBlockStore(getPathName(), objectMapper);

		decorateWorkerStore(worker);
		decorateBucketStore(buckets);
		decorateCBlockStore(cBlocks);
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


	private void decorateWorkerStore(SingletonStore<WorkerInformation> store) {
		// Nothing to decorate
	}

	private void decorateBucketStore(IdentifiableStore<Bucket> store) {
		// Nothing to decorate
	}

	private void decorateCBlockStore(IdentifiableStore<CBlock> baseStoreCreator) {
		// Nothing to decorate
	}

	// CBlocks

	public void addCBlock(CBlock cBlock) {
		log.debug("Adding CBlock[{}]", cBlock.getId());
		cBlocks.add(cBlock);
		cache.invalidate(cBlock.getId());
	}

	public CBlock getCBlock(CBlockId id) {
		return get(id);
	}

	private CBlock getCBlockFromStorage(CBlockId id) {
		return cBlocks.get(id);
	}

	public void removeCBlock(CBlockId id) {
		log.debug("Removing CBlock[{}]", id);
		cBlocks.remove(id);
		cache.invalidate(id);
	}

	public Stream<CBlock> getAllCBlocks() {
		return cBlocks.getAllKeys().map(CBlockId.class::cast).map(this::get);
	}

	public Stream<CBlockId> getAllCBlockIds() {
		return cBlocks.getAllKeys().map(CBlockId.class::cast);
	}

	// Buckets

	public void addBucket(Bucket bucket) {
		log.debug("Adding Bucket[{}]", bucket.getId());
		buckets.add(bucket);
		cache.invalidate(bucket.getId());
	}

	public Bucket getBucket(BucketId id) {
		return get(id);
	}

	private Bucket getBucketFromStorage(BucketId id) {
		return buckets.get(id);
	}

	public void removeBucket(BucketId id) {
		log.debug("Removing Bucket[{}]", id);
		buckets.remove(id);
		cache.invalidate(id);
	}

	public Stream<Bucket> getAllBuckets() {
		return buckets.getAllKeys().map(BucketId.class::cast).map(this::get);
	}

	public Stream<BucketId> getAllBucketIds() {
		return buckets.getAllKeys().map(BucketId.class::cast);
	}

	// Worker

	public WorkerInformation getWorker() {
		WorkerInformation local = cachedWorker;
		if (local == null) {
			local = worker.get();
			cachedWorker = local;
		}
		return local;
	}

	public void setWorker(WorkerInformation worker) {
		this.worker.add(worker);
	}

	public void updateWorker(WorkerInformation worker) {
		this.worker.update(worker);
	}

	// Utilities

	@Override
	protected <ID extends Id<?> & NamespacedId, VALUE extends Identifiable<?>> VALUE getFromStorage(ID id) {
		if (id instanceof BucketId castId) {
			return (VALUE) getBucketFromStorage(castId);
		}
		if (id instanceof CBlockId castId) {
			return (VALUE) getCBlockFromStorage(castId);
		}
		return super.getFromStorage(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return super.inject(values).add(WorkerStorage.class, this);
	}
}
