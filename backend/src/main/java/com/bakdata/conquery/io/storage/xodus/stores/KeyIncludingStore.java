package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.ManagedStore;
import com.bakdata.conquery.io.storage.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class KeyIncludingStore <KEY, VALUE> implements Closeable, ManagedStore {

	protected final Store<KEY, VALUE> store;

	protected abstract KEY extractKey(VALUE value);

	public void add(VALUE value) {
		store.add(extractKey(value), value);
		added(value);
	}

	public VALUE get(KEY key) {
		return store.get(key);
	}


	@Override
	public void loadKeys() {
		store.loadKeys();
	}


	public void update(VALUE value) {
		updated(value);
		store.update(extractKey(value), value);
	}

	public void remove(KEY key) {
		VALUE old = get(key);
		store.remove(key);
		if(old != null)
			removed(old);
	}

	public void loadData() {
		store.loadData();
		getAll().forEach(this::added);
	}

	public Stream<VALUE> getAll() {
		return store.getAllKeys()
					.map(this::getIgnoringExceptions)
					.filter(Objects::nonNull);
	}

	/**
	 * Gets the value for a key if it is present and can be loaded.
	 * If the value could not be loaded, returns <code>null</code>.
	 */
	private VALUE getIgnoringExceptions(KEY key) {
		try {
			return get(key);
		} catch (Exception e) {
			log.trace("Unable to load value for key {}", key, e);
			return null;
		}
	}

	public Stream<KEY> getAllKeys() {
		return store.getAllKeys();
	}

	public SerializingStore.IterationStatistic forEach(Store.StoreEntryConsumer<KEY, VALUE> consumer) {
		return store.forEach(consumer);
	}
	
	@Override
	public String toString() {
		return store.toString();
	}

	protected abstract void removed(VALUE value);

	protected abstract void added(VALUE value);

	protected abstract void updated(VALUE value);

	public void clear() {
		store.clear();
	}

	public void removeStore() {
		store.removeStore();
	}

	@Override
	public void close() throws IOException {
		store.close();
	}

	@Override
	public void invalidateCache() {
		store.invalidateCache();
	}
}
