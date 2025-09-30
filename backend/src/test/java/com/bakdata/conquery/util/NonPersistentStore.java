package com.bakdata.conquery.util;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import lombok.ToString;

@ToString
public class NonPersistentStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final ConcurrentMap<KEY, VALUE> map = new ConcurrentHashMap<>();

	@Override
	public boolean add(KEY key, VALUE value) {
		synchronized (map) {
			boolean notPresent = !map.containsKey(key);
			if(notPresent) {
				map.put(key, value);
			}
			// Was not present before
			return notPresent;
		}
	}

	@Override
	public VALUE get(KEY key) {
		return map.get(key);
	}

	@Override
	public SerializingStore.IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		final SerializingStore.IterationStatistic stats = new SerializingStore.IterationStatistic();
		map.forEach((key, value) -> {
			consumer.accept(key, value, stats.getTotalProcessed());
			stats.incrTotalProcessed();

		});
		return stats;
	}

	@Override
	public boolean update(KEY key, VALUE value) {
		map.put(key, value);
		return true;
	}

	@Override
	public boolean remove(KEY key) {
		VALUE remove = map.remove(key);
		return remove != null;
	}

	@Override
	public boolean hasKey(KEY key) {
		return map.containsKey(key);
	}

	@Override
	public int count() {
		return map.size();
	}

	@Override
	public Stream<VALUE> getAll() {
		return map.values().stream();
	}

	@Override
	public Stream<KEY> getAllKeys() {
		return map.keySet().stream();
	}

	@Override
	public void loadData() {

	}

	@Override
	public void close() throws IOException {
		// Nothing to close
	}

	@Override
	public void removeStore() {
		clear();
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void invalidateCache() {
		/* Do nothing (semantically this is not a cache, although we hold everything in memory) */
	}

	@Override
	public void loadKeys() {

	}
}
