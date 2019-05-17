package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.functions.ThrowingConsumer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent=true) @Setter @Getter
public abstract class KeyIncludingStore <KEY, VALUE> implements Closeable {

	private final Store<KEY, VALUE> store;
	protected ThrowingConsumer<VALUE> onAdd;
	protected ThrowingConsumer<VALUE> onRemove;
	
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
	
	protected void removed(VALUE value) {
		try {
			if(value != null && onRemove != null) {
				onRemove.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to remove "+value, e);
		}
	}

	protected void added(VALUE value) {
		try {
			if(value != null && onAdd != null) {
				onAdd.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
}
