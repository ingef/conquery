package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.serializer.IdReferenceResolvingException;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.util.io.ProgressBar;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.caffeine.MetricsStatsCounter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.google.common.base.Stopwatch;
import com.jakewharton.byteunits.BinaryByteUnit;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class CachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private static final ProgressBar PROGRESS_BAR = new ProgressBar(0);

	private final LoadingCache<KEY, VALUE> cache;

	/**
	 * Do not edit this directly, we have to keep it outside for synchronization purposes.
	 */
	private final Set<KEY> backingKeys = new HashSet<>();
	private final Set<KEY> keys = Collections.synchronizedSet(backingKeys);

	@ToString.Include
	private final Store<KEY, VALUE> store;

	public CachedStore(Store<KEY, VALUE> store, CaffeineSpec caffeineSpec, MetricRegistry metricRegistry) {
		this.store = store;

		final StatsCounter statsCounter =
				metricRegistry != null ? new MetricsStatsCounter(metricRegistry, "cache." + store.toString()) : StatsCounter.disabledStatsCounter();

		final Caffeine<KEY, VALUE> caffeine = Caffeine.from(caffeineSpec)
													  .recordStats(() -> statsCounter)
													  .evictionListener(
															  (k, v, cause) ->
																	  log.trace("Evicting {} from cache for {} (cause: {})", k, store.toString(), cause));

		cache = caffeine.build(this::getFromStore);
	}

	private VALUE getFromStore(KEY key) {
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final VALUE value = store.get(key);
		log.trace("Loaded {} from store {} in {}", key, store, stopwatch);

		if (value != null) {
			keys.add(key);
		}

		return value;
	}


	@Override
	public synchronized boolean add(KEY key, VALUE value) {
		final boolean added = store.add(key, value);
		if (added) {
			added(key, value);
		}
		return added;
	}

	private void added(KEY key, VALUE value) {
		cache.put(key, value);
		keys.add(key);
	}

	@Override
	public synchronized boolean update(KEY key, VALUE value) {
		final boolean update = store.update(key, value);
		added(key, value);
		return update;
	}

	@Override
	public VALUE get(KEY key) {
		return cache.get(key);
	}

	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		// There is currently no good way to use the cache here
		return store.forEach(consumer);
	}

	@Override
	public boolean remove(KEY key) {
		final boolean remove = store.remove(key);
		removed(key);
		return remove;
	}

	private void removed(KEY key) {
		cache.invalidate(key);

		keys.remove(key);
	}

	public void loadKeys() {
		final Stopwatch stopwatch = Stopwatch.createStarted();

		log.info("BEGIN loading keys {}", this);

		store.getAllKeys().forEach(keys::add);

		log.debug("DONE loading keys from {} in {}", this, stopwatch);
	}

	@Override
	public Stream<KEY> getAllKeys() {
		return (Stream<KEY>) Arrays.stream(keys.toArray());
	}

	@Override
	public void loadData() {
		final LongAdder totalSize = new LongAdder();
		final int count = count();
		final ProgressBar bar;

		if (count > 100) {
			synchronized (PROGRESS_BAR) {
				bar = PROGRESS_BAR;
				bar.addMaxValue(count);
			}
		}
		else {
			bar = null;
		}

		log.info("BEGIN loading store {}", this);


		final Stopwatch timer = Stopwatch.createStarted();

		store.forEach((key, value, size) -> {
			try {
				totalSize.add(size);
				added(key, value);
			}
			catch (RuntimeException e) {
				if (e.getCause() != null && e.getCause() instanceof IdReferenceResolvingException) {
					log.warn("Probably failed to read id '{}' because it is not yet present, skipping", ((IdReferenceResolvingException) e.getCause()).getValue(), e);
				}
				else {
					throw e;
				}
			}
			finally {
				if (bar != null) {
					bar.addCurrentValue(1);
				}
			}
		});
		log.debug("\tloaded store {}: {} entries, {} within {}", this, count, BinaryByteUnit.format(totalSize.sum()), timer.stop());
	}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public Stream<VALUE> getAll() {
		return getAllKeys().map(cache::get);
	}

	@Override
	public void clear() {
		store.clear();
		cache.invalidateAll();
		keys.clear();
	}

	@Override
	public String getName() {
		return store.getName();
	}

	@Override
	public synchronized void invalidateCache() {
		// There might be another cache we delegate to, so we invalidate that too
		store.invalidateCache();
		cache.invalidateAll();
	}

	@Override
	public void removeStore() {
		store.removeStore();
		cache.invalidateAll();
		keys.clear();
	}

	@Override
	public void close() throws IOException {
		store.close();
		cache.invalidateAll();
		keys.clear();
	}


}
