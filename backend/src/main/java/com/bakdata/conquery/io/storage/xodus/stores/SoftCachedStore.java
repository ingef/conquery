package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.dropwizard.util.Duration;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Weakly cached store, using {@link LoadingCache} to maintain values. Is a wrapper around the supplied {@link Store}.
 */
@Slf4j
@ToString(of = "store")
public class SoftCachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final LoadingCache<KEY, Optional<VALUE>> cache;

	private final Store<KEY, VALUE> store;

	public SoftCachedStore(Store<KEY, VALUE> store, Duration weakCacheDuration) {
		this.store = store;
		this.cache = CacheBuilder.newBuilder()
				.softValues()
				.expireAfterAccess(
						weakCacheDuration.getQuantity(),
						weakCacheDuration.getUnit()
				)
				.build(new CacheLoader<KEY, Optional<VALUE>>() {
					@Override
					public Optional<VALUE> load(KEY key) throws Exception {
						log.trace("Needing to load entry {} in ", key);
						return Optional.ofNullable(store.get(key));
					}
				});
	}


	@Override
	public void add(KEY key, VALUE value) {
		try {
			Optional<VALUE> old = cache.get(key);
			if(old.isPresent()) {
				throw new IllegalStateException("The id "+key+" is already part of this store");
			}
			cache.put(key, Optional.of(value));
			store.add(key, value);
		}
		catch(ExecutionException e) {
			throw new RuntimeException("Failed to load entry for key "+key, e);
		}
	}

	@Override
	public VALUE get(KEY key) {
		try {
			return cache.get(key).orElse(null);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to load entry for key "+key, e);
		}
	}

	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(KEY key, VALUE value) {
		cache.put(key, Optional.of(value));
		store.update(key, value);
	}

	@Override
	public void remove(KEY key) {
		cache.invalidate(key);
		store.remove(key);
	}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public void fillCache() {}

	@Override
	public Collection<VALUE> getAll() {
		return store.getAll();
	}

	@Override
	public Collection<KEY> getAllKeys() {
		return store.getAllKeys();
	}

	@Override
	public void clear() {
		cache.invalidateAll();
		store.clear();
	}

	@Override
	public void deleteStore() {
		cache.invalidateAll();
		store.deleteStore();
	}

	@Override
	public void close() throws IOException {
		store.close();
	}
}
