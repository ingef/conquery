package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

public interface WorkerStorage extends NamespacedStorage {
	
	WorkerInformation getWorker();
	void setWorker(WorkerInformation worker);
	void updateWorker(WorkerInformation worker);
	
	void addBucket(Bucket bucket);
	Bucket getBucket(BucketId id);
	void removeBucket(BucketId id);
	Collection<Bucket> getAllBuckets();
	
	void addCBlock(CBlock cBlock);
	CBlock getCBlock(CBlockId id);
	void updateCBlock(CBlock cBlock);
	void removeCBlock(CBlockId id);
	Collection<CBlock> getAllCBlocks();

	public static WorkerStorage tryLoad(Validator validator, XodusStorageFactory config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getXodusName(), t));
		env.close();

		if(!exists) {
			return null;
		}
		
		WorkerStorage storage = new WorkerStorageImpl(validator, directory, config);
		storage.loadData();
		return storage;
	}
	
}