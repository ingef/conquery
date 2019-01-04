package com.bakdata.conquery.io.xodus;

import java.io.File;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.models.config.StorageConfig;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter
	private final MasterMetaStorage metaStorage;
	
	public NamespaceStorageImpl(MasterMetaStorage metaStorage, StorageConfig config, File directory) {
		super(metaStorage.getValidator(), config, directory);
		this.metaStorage = metaStorage;
		this.imports = new IdentifiableStore<>(centralRegistry, StoreInfo.IMPORTS.cached(this));
	}
}
