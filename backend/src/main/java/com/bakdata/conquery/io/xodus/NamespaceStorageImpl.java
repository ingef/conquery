package com.bakdata.conquery.io.xodus;

import java.io.File;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.IdMapping;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter @Setter @NonNull
	private MasterMetaStorage metaStorage;
	private final SingletonStore<IdMapping> idMapping;
	
	public NamespaceStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
		this.imports = new IdentifiableStore<>(centralRegistry, StoreInfo.IMPORTS.cached(this));
		this.idMapping = StoreInfo.ID_MAPPING.singleton(this);
	}

	@Override
	public IdMapping getIdMapping() {
		return idMapping.get();
	}

	@Override
	public void updateIdMapping(IdMapping idMapping) throws JSONException {
		this.idMapping.update(idMapping);
	}
}
