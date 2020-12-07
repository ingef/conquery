package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.Set;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;

public interface NamespaceStorage extends NamespacedStorage {
	
	public static NamespaceStorage tryLoad(Validator validator, StorageConfig config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.DATASET.getXodusName(), t));
		env.close();

		if(!exists) {
			return null;
		}

		NamespaceStorage storage = new NamespaceStorageImpl(validator, config, directory);
		storage.loadData();
		return storage;
	}
	
	MetaStorage getMetaStorage();
	void setMetaStorage(@NonNull MetaStorage storage);
	
	StructureNode[] getStructure();
	void updateStructure(StructureNode[] structure) throws JSONException;
	
	PersistentIdMap getIdMapping();
	void updateIdMapping(PersistentIdMap idMap) throws JSONException;

	void setWorkerToBucketsMap(WorkerToBucketsMap map);
	WorkerToBucketsMap getWorkerBuckets();

	Set<SecondaryIdDescription> getSecondaryIds();
	void setSecondaryIds();

}