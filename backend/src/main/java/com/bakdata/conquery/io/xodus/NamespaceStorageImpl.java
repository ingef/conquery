package com.bakdata.conquery.io.xodus;

import com.bakdata.conquery.models.config.StorageConfig;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validator;
import java.io.File;

@Slf4j
public class NamespaceStorageImpl extends NamespacedStorageImpl implements NamespaceStorage {
	
	@Getter @Setter @NonNull
	private MasterMetaStorage metaStorage;
	
	public NamespaceStorageImpl(Validator validator, StorageConfig config, File directory) {
		super(validator, config, directory);
	}
}
