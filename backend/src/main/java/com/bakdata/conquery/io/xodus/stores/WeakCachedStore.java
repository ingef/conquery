package com.bakdata.conquery.io.xodus.stores;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.SerializingStore.IterationStatistic;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

/**
 * Weakly cached store, using {@link LoadingCache} to maintain values. Is a wrapper around the supplied {@link Store}.
 */
@Slf4j
public class WeakCachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final LoadingCache<KEY, Optional<VALUE>> cache;

	private final Store<KEY, VALUE> store;

	public WeakCachedStore(Store<KEY, VALUE> store, Duration weakCacheDuration) {
		this.store = store;
		this.cache = CacheBuilder.newBuilder()
				.weakValues()
				.expireAfterAccess(
						weakCacheDuration.getQuantity(),
						weakCacheDuration.getUnit()
				)
				.build(new CacheLoader<KEY, Optional<VALUE>>() {
					@Override
					public Optional<VALUE> load(KEY key) throws Exception {
						log.trace("Needing to load entry "+key+" in "+this);
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
	public void inject(Injectable injectable) {
		store.inject(injectable);
	}
	
	@Override
	public String toString() {
		return "weakcached "+store.toString();
	}


	@Override
	public void clear() {
		cache.invalidateAll();
		store.clear();
	}

	@Override
	public void remove() {
		cache.invalidateAll();
		store.remove();
	}
}
