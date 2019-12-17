package com.bakdata.conquery.io.xodus;

import javax.validation.Validator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.util.functions.Collector;
import com.google.common.base.Stopwatch;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter @Slf4j
public abstract class ConqueryStorageImpl implements ConqueryStorage {

	protected final File directory;
	protected final Validator validator;
	protected final Environment environment;
	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
	private final List<KeyIncludingStore<?,?>> stores = new ArrayList<>();

	public ConqueryStorageImpl(Validator validator, StorageConfig config, File directory) {
		this.directory = directory;
		this.validator = validator;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
	}

	protected void createStores(Collector<KeyIncludingStore<?,?>> collector) {
	}
	
	@Override
	public void loadData() {
		createStores(stores::add);
		log.info("Loading storage {} from {}", this.getClass().getSimpleName(), directory);
		Stopwatch all = Stopwatch.createStarted();
		for(KeyIncludingStore<?, ?> store : stores) {
			store.loadData();
		}
		log.info("Loaded complete {} storage within {}", this.getClass().getSimpleName(), all.stop());
	}

	@Override
	public void close() throws IOException {
		for(KeyIncludingStore<?, ?> store : stores) {
			store.close();
		}
		environment.close();
	}

	/**
	 * clears the environment the closes it.
	 * @throws IOException
	 */
	public void remove() throws IOException {
		environment.clear();
		close();
	}
}
