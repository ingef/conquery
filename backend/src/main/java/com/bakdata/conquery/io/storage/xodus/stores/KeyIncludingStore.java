package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.Closeable;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.ManagedStore;
import com.bakdata.conquery.io.storage.Store;
import lombok.Data;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class KeyIncludingStore<KEY, VALUE> implements Closeable, ManagedStore {

	@Delegate
	protected final Store<KEY, VALUE> store;

	public void add(VALUE value) {
		store.add(extractKey(value), value);
	}

	protected abstract KEY extractKey(VALUE value);


	public void update(VALUE value) {
		store.update(extractKey(value), value);
	}

	public VALUE get(KEY key) {
		return store.get(key);
	}

	public Stream<VALUE> getAll() {
		return store.getAllKeys()
					.map(this::getIgnoringExceptions)
					.filter(Objects::nonNull);
	}

	/**
	 * Gets the value for a key if it is present and can be loaded.
	 * If the value could not be loaded, returns <code>null</code>.
	 */
	private VALUE getIgnoringExceptions(KEY key) {
		try {
			return get(key);
		}
		catch (Exception e) {
			log.trace("Unable to load value for key {}", key, e);
			return null;
		}
	}

    public boolean contains(KEY key) {
		return store.contains(key);
    }
}
