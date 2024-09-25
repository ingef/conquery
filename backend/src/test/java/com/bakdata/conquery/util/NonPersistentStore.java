package com.bakdata.conquery.util;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;
import lombok.ToString;

@ToString
public class NonPersistentStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private final ConcurrentMap<KEY, VALUE> map = new ConcurrentHashMap<>();

	@Override
	public void add(KEY key, VALUE value) {
		map.put(key, value);
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
	public void update(KEY key, VALUE value) {
		map.put(key, value);
	}

	@Override
	public void remove(KEY key) {
		map.remove(key);
	}

	@Override
	public void loadData() {

	}

	@Override
	public int count() {
		return map.size();
	}

	@Override
	public Collection<VALUE> getAll() {
		return map.values();
	}

	@Override
	public Collection<KEY> getAllKeys() {
		return map.keySet();
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
	public void close() throws IOException {
		// Nothing to close
	}
}
