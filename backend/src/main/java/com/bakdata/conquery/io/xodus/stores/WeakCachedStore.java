package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.SerializingStore.IterationResult;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Weakly cached store, using {@link LoadingCache} to maintain values. Is a wrapper around the supplied {@link Store}.
 */
@RequiredArgsConstructor @Slf4j
public class WeakCachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private LoadingCache<KEY, Optional<VALUE>> cache = CacheBuilder.newBuilder()
		.weakValues()
		.expireAfterAccess(
			ConqueryConfig.getInstance().getStorage().getWeakCacheDuration().getQuantity(),
			ConqueryConfig.getInstance().getStorage().getWeakCacheDuration().getUnit()
		)
		.build(new CacheLoader<KEY, Optional<VALUE>>() {
			@Override
			public Optional<VALUE> load(KEY key) throws Exception {
				log.trace("Needing to load entry "+key+" in "+this);
				return Optional.ofNullable(store.get(key));
			}
		});

	private final Store<KEY, VALUE> store;
	
	@Override
	public void close() throws IOException {
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
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
	public IterationResult forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
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
}
