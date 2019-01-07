package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Stopwatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class CachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final static ProgressBar PROGRESS_BAR = new ProgressBar(0, System.out);
	
	private final ConcurrentHashMap<KEY, VALUE> cache = new ConcurrentHashMap<>();
	private final Store<KEY, VALUE> store;
	
	@Override
	public void close() throws IOException {
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		if(cache.putIfAbsent(key, value)!=null) {
			throw new IllegalStateException("The id "+key+" is alread part of this store");
		}
		store.add(key, value);
	}

	@Override
	public VALUE get(KEY key) {
		return cache.get(key);
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
		cache.remove(key);
		store.remove(key);
	}
	
	@Override
	public int count() {
		if(cache.isEmpty()) {
			return store.count();
		}
		else {
			return cache.size();
		}
	}

	@Override
	public void fillCache() {
		AtomicLong totalSize = new AtomicLong(0);
		long count = count();
		final ProgressBar bar;
		Stopwatch timer = Stopwatch.createStarted();
		
		if(count>100) {
			synchronized (PROGRESS_BAR) {
				bar = PROGRESS_BAR;
				bar.addMaxValue(count);
			}
			log.info("\tloading store {}", this);
		}
		else {
			bar = null;
		}
		
		store.forEach(entry -> {
			totalSize.addAndGet(entry.getByteSize());
			cache.put(entry.getKey(), entry.getValue());
			if(bar != null) {
				bar.addCurrentValue(1);
			}
		});
		log.info(
				"\tloaded store {}\n\tentries: {}\n\tsize: {}\n\tloading time: {}",
				this,
				cache.values().size(),
				FileUtils.byteCountToDisplaySize(totalSize.get()),
				timer.stop()
		);
	}

	@Override
	public Collection<VALUE> getAll() {
		return cache.values();
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
