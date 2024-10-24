package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.serializer.IdReferenceResolvingException;
import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.util.io.ProgressBar;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Stopwatch;
import com.jakewharton.byteunits.BinaryByteUnit;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class CachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private static final ProgressBar PROGRESS_BAR = new ProgressBar(0, System.out);

	private final LoadingCache<KEY, VALUE> cache;
	@ToString.Include
	private final Store<KEY, VALUE> store;

	public CachedStore(Store<KEY, VALUE> store, CaffeineSpec caffeineSpec) {
		this.store = store;

		cache = Caffeine.from(caffeineSpec)
//						.recordStats(() -> new MetricsStatsCounter(metricRegistry, "cache."+store.toString()))
						.build(this.store::get);
	}

	@Override
	public void add(KEY key, VALUE value) {
		// We don't distinguish between add and update on this layer. Let a deeper layer complain
		update(key, value);
	}

	@Override
	public synchronized void update(KEY key, VALUE value) {
		store.update(key, value);
		cache.put(key, value);
	}

	@Override
	public VALUE get(KEY key) {
		return cache.get(key);
	}

	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		store.getAllKeys().forEach( k -> consumer.accept(k, cache.get(k), 0 /*Leaky?*/));
		return null;
	}

	@Override
	public void remove(KEY key) {
		store.remove(key);
		cache.invalidate(key);
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
				cache.put(key, value);
			}
			catch (RuntimeException e) {
				if (e.getCause() != null && e.getCause() instanceof IdReferenceResolvingException) {
					log.warn(
							"Probably failed to read id '{}' because it is not yet present, skipping",
							((IdReferenceResolvingException) e.getCause()).getValue(),
							e
					);
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
		log.debug("\tloaded store {}: {} entries, {} within {}",
				this,
			  	count,
				BinaryByteUnit.format(totalSize.sum()),
				timer.stop()
		);
	}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public Stream<VALUE> getAll() {
		return store.getAllKeys().map(cache::get);
	}

	@Override
	public Stream<KEY> getAllKeys() {
		return store.getAllKeys();
	}

	@Override
	public void clear() {
		store.clear();
		cache.invalidateAll();
	}

	@Override
	public void removeStore() {
		store.removeStore();
		cache.invalidateAll();
	}

	@Override
	public void close() throws IOException {
		store.close();
	}
}
