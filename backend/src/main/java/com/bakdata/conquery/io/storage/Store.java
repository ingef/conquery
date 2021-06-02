package com.bakdata.conquery.io.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.function.BiConsumer;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore.IterationStatistic;

public interface Store<KEY, VALUE> {

	public void add(KEY key, VALUE value);

	public VALUE get(KEY key);

	public IterationStatistic forEach(BiConsumer<KEY, VALUE> consumer);

	// TODO: 08.01.2020 fk: Is this still necessary? The implementation in XodusStore uses different methods that in our context don't act differently.
	public void update(KEY key, VALUE value);
	
	public void remove(KEY key);

	public void fillCache();
	
	public int count();

	public Collection<VALUE> getAll();

	public void inject(Injectable injectable);

	public Collection<KEY> getAllKeys();

	void clear();

	void removeStore();

	void close() throws IOException;
}
