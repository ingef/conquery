package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;

@Getter
public abstract class ConqueryStorageImpl implements ConqueryStorage {

	protected final File directory;
	protected final Validator validator;
	protected final Environment environment;
	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();

	public ConqueryStorageImpl(Validator validator, StorageConfig config, File directory) {
		this.directory = directory;
		this.validator = validator;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
	}

	protected void stopStores() throws IOException {}

	@Override
	public void close() throws IOException {
		stopStores();
		environment.close();
	}
}
