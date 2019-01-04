package com.bakdata.conquery.io.xodus;

import java.io.File;

import com.bakdata.conquery.models.config.StorageConfig;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

public interface NamespaceStorage extends NamespacedStorage {
	
	public static NamespaceStorage tryLoad(MasterMetaStorage metaStorage, StorageConfig config, File directory) {
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		boolean exists = env.computeInTransaction(t->env.storeExists(StoreInfo.NAMESPACES.getXodusName(), t));
		env.close();

		if(!exists) {
			return null;
		}
		
		return new NamespaceStorageImpl(metaStorage, config, directory);
	}
	
	MasterMetaStorage getMetaStorage();
}