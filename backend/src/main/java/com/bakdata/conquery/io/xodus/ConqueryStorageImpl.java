package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.metrics.JobMetrics;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.util.functions.Collector;
import com.codahale.metrics.Timer;
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
	protected final List<KeyIncludingStore<?,?>> stores = new ArrayList<>();
	@Getter
	private final int nThreads;

	public ConqueryStorageImpl(Validator validator, StorageConfig config, File directory) {
		this.directory = directory;
		this.validator = validator;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
		this.nThreads = config.getThreads();
	}

	protected void createStores(Collector<KeyIncludingStore<?,?>> collector) {
	}

	/**
	 * Load all stores from disk.
	 */
	@Override
	public void loadData() {
		createStores(stores::add);
		log.info("Loading storage {} from {}", this.getClass().getSimpleName(), directory);

		try (final Timer.Context timer = JobMetrics.getStoreLoadingTimer()) {

			Stopwatch all = Stopwatch.createStarted();
			for (KeyIncludingStore<?, ?> store : stores) {
				store.loadData();
			}

			log.info("Loaded complete {} storage within {}", this.getClass().getSimpleName(), all.stop());
		}
	}

	@Override
	public void close() throws IOException {
		for(KeyIncludingStore<?, ?> store : stores) {
			store.close();
		}
		environment.close();
	}

	/**
	 * Clears the environment then closes it.
	 */
	public void remove() throws IOException {
		environment.clear();
		close();
	}
}
