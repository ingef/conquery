package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.ManagedStore;
import com.bakdata.conquery.io.storage.Store;

public abstract class KeyIncludingStore <KEY, VALUE> implements Closeable, ManagedStore {

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
					.map(store::get);
	}

	public Stream<KEY> getAllKeys() {
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
