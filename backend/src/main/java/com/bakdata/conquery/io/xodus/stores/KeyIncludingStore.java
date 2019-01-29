package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.bakdata.conquery.models.exceptions.JSONException;

public abstract class KeyIncludingStore <KEY, VALUE> implements Closeable {

	private final Store<KEY, VALUE> store;
	
	public KeyIncludingStore(Store<KEY, VALUE> store) {
		this.store = store;
	}
	
	@Override
	public void close() throws IOException {
		store.close();
	}
	
	protected abstract KEY extractKey(VALUE value);
	
	public void add(VALUE value) throws JSONException {
		store.add(extractKey(value), value);
		added(value);
	}
	
	public VALUE get(KEY key) {
		return store.get(key);
	}

	public void forEach(Consumer<VALUE> consumer) {
		store.forEach(e -> consumer.accept(e.getValue()));
	}

	public void update(VALUE value) throws JSONException {
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
	
	@Override
	public String toString() {
		return store.toString();
	}
	
	protected abstract void removed(VALUE old);
	protected abstract void added(VALUE value);
}
