package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.IOException;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Weakly cached store, using {@link LoadingCache} to maintain values. Is a wrapper around the supplied {@link Store}.
 */
@Slf4j
public class CachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final LoadingCache<KEY, VALUE> cache;

	private final Store<KEY, VALUE> store;

	public CachedStore(Store<KEY, VALUE> store, Duration weakCacheDuration) {
		this.store = store;
		this.cache = Caffeine.newBuilder()
							 .weakValues()
							 .expireAfterAccess(
									 weakCacheDuration.getQuantity(),
									 weakCacheDuration.getUnit()
							 )
							 .removalListener(new RemovalListener<KEY, VALUE>() {
								 @Override
								 public void onRemoval(@Nullable KEY key, @Nullable VALUE value, RemovalCause cause) {
									 if (!cause.wasEvicted()) {
										 store.remove(key);
										 log.trace("Removed {} from underlying store. Value: {}", key, value);
									 }
								 }
							 })
							 .build(new CacheLoader<KEY, VALUE>() {
								 @Override
								 public VALUE load(KEY key) {
									 log.trace("Needing to load entry " + key + " in " + this);
									 return store.get(key);
								 }
							 });
	}


	@Override
	public void add(KEY key, VALUE value) {
		VALUE old = cache.get(key);
		if (old != null) {
			throw new IllegalStateException("The id " + key + " is already part of this store");
		}
		cache.put(key, value);
		store.add(key, value);
	}

	@Override
	public VALUE get(KEY key) {
		return cache.get(key);
	}

	@Override
	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(KEY key, VALUE value) {
		synchronized (this) {
			cache.put(key, value);
			store.update(key, value);
		}
	}

	@Override
	public void remove(KEY key) {
		cache.invalidate(key);
	}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public void loadData() {
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
	public String toString() {
		return "cached " + store.toString();
	}


	@Override
	public void clear() {
		cache.invalidateAll();
		store.clear();
	}

	@Override
	public void removeStore() {
		cache.invalidateAll();
		store.removeStore();
	}

	@Override
	public void close() throws IOException {
		store.close();
	}
}
