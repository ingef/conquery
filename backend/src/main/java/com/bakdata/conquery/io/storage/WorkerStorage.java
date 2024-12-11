package com.bakdata.conquery.io.storage;

import java.io.Closeable;
import java.util.stream.Stream;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.WorkerInformation;
public interface WorkerStorage extends NamespacedStorage, Closeable {
	void addCBlock(CBlock cBlock);

	CBlock getCBlock(CBlockId id);

	void removeCBlock(CBlockId id);

	Stream<CBlock> getAllCBlocks();

	Stream<CBlockId> getAllCBlockIds();

	void addBucket(Bucket bucket);

	Bucket getBucket(BucketId id);

	void removeBucket(BucketId id);

	Stream<Bucket> getAllBuckets();

	Stream<BucketId> getAllBucketIds();

	WorkerInformation getWorker();

	void setWorker(WorkerInformation worker);

	void updateWorker(WorkerInformation worker);

	void forEachBucket(Store.StoreEntryConsumer<Id<Bucket>, Bucket> consumer);

	void forEachCBlock(Store.StoreEntryConsumer<Id<CBlock>, CBlock> consumer);
}
