package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(of = "worker")
public class WorkerStorageImpl extends NamespacedStorageImpl implements WorkerStorage {

	private SingletonStore<WorkerInformation> worker;
	private IdentifiableStore<Bucket> buckets;
	private IdentifiableStore<CBlock> cBlocks;

	public WorkerStorageImpl(StoreFactory storageFactory, String pathName) {
		super(storageFactory, pathName);
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(
				dataset,
				secondaryIds,
				tables,
				imports,
				concepts,

				entity2Bucket,
				worker,
				buckets,
				cBlocks
		);
	}

	@Override
	public void openStores(ObjectMapper objectMapper) {
		super.openStores(objectMapper);

		worker = getStorageFactory().createWorkerInformationStore(getPathName(), objectMapper);
		buckets = getStorageFactory().createBucketStore(getPathName(), objectMapper);
		cBlocks = getStorageFactory().createCBlockStore(getPathName(), objectMapper);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return super.inject(values).add(WorkerStorage.class, this);
	}

	@Override
	public void addCBlock(CBlock cBlock) {
		log.trace("Adding CBlock[{}]", cBlock.getId());
		cBlocks.add(cBlock);
	}

	@Override
	public void removeCBlock(CBlockId id) {
		log.trace("Removing CBlock[{}]", id);
		cBlocks.remove(id);
	}

	@Override
	public Stream<CBlock> getAllCBlocks() {
		return cBlocks.getAllKeys().map(CBlockId.class::cast).map(CBlockId::resolve);
	}

	@Override
	public CBlock getCBlock(CBlockId id) {
		return cBlocks.get(id);
	}

	@Override
	public Stream<CBlockId> getAllCBlockIds() {
		return cBlocks.getAllKeys().map(CBlockId.class::cast);
	}


	@Override
	public void addBucket(Bucket bucket) {
		log.trace("Adding Bucket[{}]", bucket.getId());
		buckets.add(bucket);
	}

	@Override
	public void removeBucket(BucketId id) {
		log.trace("Removing Bucket[{}]", id);
		buckets.remove(id);
	}

	@Override
	public Bucket getBucket(BucketId id) {
		return buckets.get(id);
	}

	@Override
	public Stream<BucketId> getAllBucketIds() {
		return buckets.getAllKeys().map(BucketId.class::cast);
	}


	@Override
	public WorkerInformation getWorker() {
		return worker.get();
	}

	@Override
	public void setWorker(WorkerInformation worker) {
		this.worker.add(worker);
	}

	@Override
	public void updateWorker(WorkerInformation worker) {
		this.worker.update(worker);
	}
  
	public Stream<String> getAllEntities() {
		return entity2Bucket.getAllKeys();
	}

	public boolean hasCBlock(CBlockId id) {
		return cBlocks.hasKey(id);
	}
}
