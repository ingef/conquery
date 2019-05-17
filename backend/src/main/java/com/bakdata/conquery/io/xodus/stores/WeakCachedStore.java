package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class WeakCachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private LoadingCache<KEY, VALUE> cache = CacheBuilder.newBuilder()
		.weakValues()
		.expireAfterAccess(
			ConqueryConfig.getInstance().getStorage().getWeakCacheDuration().getQuantity(),
			ConqueryConfig.getInstance().getStorage().getWeakCacheDuration().getUnit()
		)
		.build(new CacheLoader<KEY, VALUE>() {
			@Override
			public VALUE load(KEY key) throws Exception {
				log.debug("Needing to load entry "+key+" in "+this);
				return store.get(key);
			}
		});

	private final Store<KEY, VALUE> store;
	
	@Override
	public void close() throws IOException {
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		try {
			VALUE old = cache.get(key);
			if(old!=null) {
				throw new IllegalStateException("The id "+key+" is alread part of this store");
			}
			cache.put(key, value);
			store.add(key, value);
		}
		catch(ExecutionException e) {
			throw new RuntimeException("Failed to load entry for key "+key, e);
		}
	}

	@Override
	public VALUE get(KEY key) {
		try {
			return cache.get(key);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Failed to load entry for key "+key, e);
		}
	}

	@Override
	public void forEach(Consumer<StoreEntry<KEY, VALUE>> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		cache.put(key, value);
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
	public void inject(Injectable injectable) {
		store.inject(injectable);
	}
	
	@Override
	public String toString() {
		return "cached "+store.toString();
	}
}
