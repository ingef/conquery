package com.bakdata.conquery.io.xodus.stores;

import java.util.Collection;
import java.util.function.Consumer;

import com.bakdata.conquery.models.exceptions.JSONException;

public abstract class KeyIncludingStore <KEY, VALUE> {

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
		VALUE old = get(extractKey(value));
		if(old != null)
			removed(old);
		store.update(extractKey(value), value);
		added(value);
	}
	
	public void remove(KEY key) {
		VALUE old = get(key);
		store.remove(key);
		if(old != null)
			removed(old);
	}
	
	public void loadData() {
		store.fillCache();
		for(VALUE value : getAll()) {
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
}
