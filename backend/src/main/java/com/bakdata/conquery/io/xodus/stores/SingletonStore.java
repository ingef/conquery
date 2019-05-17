package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.util.functions.ThrowingConsumer;

import lombok.Setter;
import lombok.experimental.Accessors;

public class SingletonStore<VALUE> extends KeyIncludingStore<Boolean, VALUE> {

	public SingletonStore(Store<Boolean, VALUE> store, Injectable... injectables) {
		super(store);
		for(Injectable injectable : injectables) {
			store.inject(injectable);
		}
	}

	@Override
	protected Boolean extractKey(VALUE value) {
		return Boolean.TRUE;
	}

	@Override @Deprecated
	public VALUE get(Boolean key) {
		return get();
	}
	
	public synchronized VALUE get() {
		return super.get(Boolean.TRUE);
	}
	
	@Override @Deprecated
	public void remove(Boolean key) {
		remove();
	}
	
	public synchronized void remove() {
		super.remove(Boolean.TRUE);
	}
}
