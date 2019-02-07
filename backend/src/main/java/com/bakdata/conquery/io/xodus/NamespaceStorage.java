package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.CsvId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalId;
import com.bakdata.conquery.models.identifiable.mapping.PersistingIdMap;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;

import javax.validation.Validator;
import java.io.File;

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
	
	MasterMetaStorage getMetaStorage();
	void setMetaStorage(@NonNull MasterMetaStorage storage);
	
	StructureNode[] getStructure();
	void updateStructure(StructureNode[] structure) throws JSONException;
	
	PersistingIdMap getIdMapping();
	void updateIdMapping(PersistingIdMap idMap) throws JSONException;
}