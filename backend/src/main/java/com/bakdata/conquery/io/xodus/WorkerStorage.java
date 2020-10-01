package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

public interface WorkerStorage extends NamespacedStorage {
	
	WorkerInformation getWorker();
	void setWorker(WorkerInformation worker);
	void updateWorker(WorkerInformation worker) throws JSONException;
	
	void addBucket(Bucket bucket) throws JSONException;
	Bucket getBucket(BucketId id);
	void removeBucket(BucketId id);
	Collection<Bucket> getAllBuckets();
	
	void addCBlock(CBlock cBlock) throws JSONException;
	CBlock getCBlock(CBlockId id);
	void updateCBlock(CBlock cBlock) throws JSONException;
	void removeCBlock(CBlockId id);
	Collection<CBlock> getAllCBlocks();
	public Collection<ImportId> getTableImports(TableId tableId);

	// todo consider moving this to BucketManager as that already contains such logic.
	public void registerTableImport(ImportId impId) ;
	public void unregisterTableImport(ImportId impId) ;

	public static WorkerStorage tryLoad(Validator validator, StorageConfig config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getXodusName(), t));
		env.close();

		if(!exists) {
			return null;
		}
		
		WorkerStorage storage = new WorkerStorageImpl(validator, config, directory);
		storage.loadData();
		return storage;
	}
	
	void setBucketManager(BucketManager bucketManager);
	BucketManager getBucketManager();
}