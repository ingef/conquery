package com.bakdata.conquery.io.xodus;

import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.WorkerToBucketsMap;
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


	default boolean isRegisterImports() {
		return true;
	}
}