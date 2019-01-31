package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.BlockManager;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.BlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.worker.WorkerInformation;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

public interface WorkerStorage extends NamespacedStorage {
	
	WorkerInformation getWorker();
	void setWorker(WorkerInformation worker) throws JSONException;
	void updateWorker(WorkerInformation worker) throws JSONException;
	
	void addBlocks(List<Block> newBlocks) throws JSONException;
	Block getBlock(BlockId id);
	void removeBlock(BlockId id);
	Collection<Block> getAllBlocks();
	
	void addCBlock(CBlock cBlock) throws JSONException;
	CBlock getCBlock(CBlockId id);
	void updateCBlock(CBlock cBlock) throws JSONException;
	void removeCBlock(CBlockId id);
	Collection<CBlock> getAllCBlocks();
	
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
	
	void setBlockManager(BlockManager blockManager);
	BlockManager getBlockManager();
}