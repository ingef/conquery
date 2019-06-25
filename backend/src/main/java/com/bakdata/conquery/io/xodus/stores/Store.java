package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.util.Collection;
import java.util.function.Consumer;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.exceptions.JSONException;

public interface Store<KEY, VALUE> extends Closeable {

	public void add(KEY key, VALUE value) throws JSONException;

	public VALUE get(KEY key);

	public void forEach(Consumer<StoreEntry<KEY, VALUE>> consumer);

	public void update(KEY key, VALUE value) throws JSONException;
	
	public void remove(KEY key);

	public void fillCache();
	
	public int count();

	public Collection<VALUE> getAll();

	public void inject(Injectable injectable);

	public Collection<KEY> getAllKeys();
}
