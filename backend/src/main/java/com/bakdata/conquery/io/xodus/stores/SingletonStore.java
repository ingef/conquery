package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.util.functions.ThrowingConsumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Store for storing only a single value.
 */
@Accessors(fluent=true) @Setter @Getter
public class SingletonStore<VALUE> extends KeyIncludingStore<Boolean, VALUE> {

	@NonNull
	protected ThrowingConsumer<VALUE> onAdd = (v) -> {};

	@NonNull
	protected ThrowingConsumer<VALUE> onRemove = (v) -> {};
	
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
	
	protected void removed(VALUE value) {
		try {
			if(value != null) {
				onRemove.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to remove "+value, e);
		}
	}

	protected void added(VALUE value) {
		try {
			if(value != null) {
				onAdd.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
}
