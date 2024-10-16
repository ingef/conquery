package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;

public interface Store<KEY, VALUE> extends ManagedStore {

	public void add(KEY key, VALUE value);

	public VALUE get(KEY key);

	public IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer);

	// TODO: 08.01.2020 fk: Is this still necessary? The implementation in XodusStore uses different methods that in our context don't act differently.
	public void update(KEY key, VALUE value);

	public void remove(KEY key);


	public int count();

	public Stream<VALUE> getAll();

	public Stream<KEY> getAllKeys();

	/**
	 * Consumer of key-value pairs stored in this Store. Used in conjunction with for-each.
	 */
	@FunctionalInterface
	public interface StoreEntryConsumer<KEY, VALUE> {
		public void accept(KEY key, VALUE value, long size);
	}

	void clear();

}
