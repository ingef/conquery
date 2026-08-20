package com.bakdata.conquery.io.storage.xodus.stores;

import com.bakdata.conquery.io.storage.Store;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Store for storing only a single value.
 */
@Accessors(fluent = true)
@Setter
@Getter
public class SingletonStore<VALUE> extends KeyIncludingStore<Boolean, VALUE> {

	private static final Boolean KEY = Boolean.TRUE;


	public SingletonStore(Store<Boolean, VALUE> store) {
		super(store);
	}

	@Override
	protected Boolean extractKey(VALUE value) {
		return KEY;
	}

	@Override
	@Deprecated
	public VALUE get(Boolean key) {
		return get();
	}

	public VALUE get() {
		return super.get(KEY);
	}

	@Override
	@Deprecated
	public boolean remove(Boolean key) {
		return remove();
	}

	public boolean remove() {
		return super.remove(KEY);
	}

}
