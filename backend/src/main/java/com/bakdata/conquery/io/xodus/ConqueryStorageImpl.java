package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.metrics.JobMetrics;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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

	protected abstract List<ListenableFuture<KeyIncludingStore<?,?>>> createStores(ListeningExecutorService pool)
			throws ExecutionException, InterruptedException;

	/**
	 * Load all stores from disk.
	 *
	 * Create a ThreadPool that can be used to submit as many tasks in parallel as possible.
	 */
	@Override
	public final void loadData() {
		log.info("Loading storage {} from {}", this.getClass().getSimpleName(), directory);

		try (final Timer.Context timer = JobMetrics.getStoreLoadingTimer()) {
			ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(getNThreads()));

			Stopwatch all = Stopwatch.createStarted();

			final List<ListenableFuture<KeyIncludingStore<?,?>>> loaded = createStores(pool);

			stores.addAll(Futures.allAsList(loaded).get());

			pool.shutdown();
			if(!pool.awaitTermination(1, TimeUnit.DAYS)){
				throw new IllegalStateException("Some tasks have not finished loading.");
			}

			log.info("Loaded complete {} storage within {}", this.getClass().getSimpleName(), all.stop());
		}
		catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Failed loading storage.", e);
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
