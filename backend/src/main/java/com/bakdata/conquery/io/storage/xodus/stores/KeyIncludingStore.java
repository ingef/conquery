package com.bakdata.conquery.io.storage.xodus.stores;

import com.bakdata.conquery.io.storage.Store;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

public abstract class KeyIncludingStore<KEY, VALUE> implements Closeable {

	protected final Store<KEY, VALUE> store;

	public KeyIncludingStore(Store<KEY, VALUE> store) {
		this.store = store;
	}

	protected abstract KEY extractKey(VALUE value);

	public void add(VALUE value) {
		store.add(extractKey(value), value);
		added(value);
	}

	public VALUE get(KEY key) {
		return store.get(key);
	}

	public void forEach(Consumer<VALUE> consumer) {
		store.forEach((key, value, size) -> consumer.accept(value));
	}

	public void update(VALUE value) {
		updated(value);
		store.update(extractKey(value), value);
	}

	public void remove(KEY key) {
		VALUE old = get(key);
		store.remove(key);
		if (old != null)
			removed(old);
	}

	public void loadData() {
		store.fillCache();
		for (VALUE value : getAll()) {
			added(value);
		}
	}

	public Collection<VALUE> getAll() {
		return store.getAll();
	}

	public Collection<KEY> getAllKeys() {
		return store.getAllKeys();
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
}
