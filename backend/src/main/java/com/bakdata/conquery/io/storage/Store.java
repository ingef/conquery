package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;

public interface Store<KEY, VALUE> extends ManagedStore {

	/**
	 * Adds a value to the store, if the key was not present.
	 * @return True if the value was added
	 */
	boolean add(KEY key, VALUE value);

	VALUE get(KEY key);

	IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer);

	// TODO: 08.01.2020 fk: Is this still necessary? The implementation in XodusStore uses different methods that in our context don't act differently.
	boolean update(KEY key, VALUE value);

	boolean remove(KEY key);


	int count();

	Stream<VALUE> getAll();

	Stream<KEY> getAllKeys();

	void clear();

	String getName();

	boolean contains(KEY key);

	/**
	 * Consumer of key-value pairs stored in this Store. Used in conjunction with for-each.
	 */
	@FunctionalInterface
	interface StoreEntryConsumer<KEY, VALUE> {
		void accept(KEY key, VALUE value, long size);
	}

}
