package com.bakdata.conquery.io.xodus;

import java.io.File;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.XodusStorageFactory;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;

public interface NamespaceStorage extends NamespacedStorage {
	
	MetaStorage getMetaStorage();
	void setMetaStorage(@NonNull MetaStorage storage);
	
	StructureNode[] getStructure();
	void updateStructure(StructureNode[] structure) throws JSONException;
	
	PersistentIdMap getIdMapping();
	void updateIdMapping(PersistentIdMap idMap) throws JSONException;

	void setWorkerToBucketsMap(WorkerToBucketsMap map);
	WorkerToBucketsMap getWorkerBuckets();
}