package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.util.Collection;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.xodus.stores.SerializingStore.IterationResult;
import com.bakdata.conquery.models.exceptions.JSONException;

public interface Store<KEY, VALUE> extends Closeable {

	public void add(KEY key, VALUE value) throws JSONException;

	public VALUE get(KEY key);

	public IterationResult forEach(StoreEntryConsumer<KEY, VALUE> consumer);

	// TODO: 08.01.2020 fk: Is this still necessary? The implementation in XodusStore uses different methods that in our context don't act differently.
	public void update(KEY key, VALUE value) throws JSONException;
	
	public void remove(KEY key);

	public void fillCache();
	
	public int count();

	public Collection<VALUE> getAll();

	public void inject(Injectable injectable);

	public Collection<KEY> getAllKeys();

	/**
	 * Consumer of key-value pairs stored in this Store. Used in conjunction with for-each.
	 */
	@FunctionalInterface
	public interface StoreEntryConsumer<KEY, VALUE> {
		public void accept(KEY key, VALUE value, long size);
	}
}
