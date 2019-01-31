package com.bakdata.conquery.io.xodus;

import java.io.File;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.StorageConfig;

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
	
	MasterMetaStorage getMetaStorage();
	void setMetaStorage(@NonNull MasterMetaStorage storage);
}