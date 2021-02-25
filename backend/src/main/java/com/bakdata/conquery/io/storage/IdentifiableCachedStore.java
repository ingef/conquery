package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Registers accessors of values instead of the value itself to the central registry.
 * Might be useful if the object are very large and should only be loaded on demand.
 */
@Accessors(fluent=true) @Setter @Getter
public class IdentifiableCachedStore<VALUE extends Identifiable<?>> extends IdentifiableStore<VALUE> {

	public IdentifiableCachedStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store, Injectable... injectables) {
		super(store, centralRegistry);
		for(Injectable injectable : injectables) {
			store.inject(injectable);
		}
	}

	@Override
	protected IId<VALUE> extractKey(VALUE value) {
		return (IId<VALUE>)value.getId();
	}
	
	@Override
	protected void removed(VALUE value) {
		try {
			if(value != null) {
				centralRegistry.remove(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to remove "+value, e);
		}
	}

	@Override
	protected void added(VALUE value) {
		try {
			if(value != null) {
				final IId<VALUE> key = extractKey(value);
				centralRegistry.registerCacheable(key, this::get);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
	
	@Override
	public void loadData() {
		store.fillCache();
		for(IId<VALUE> key : getAllKeys()) {
			centralRegistry.registerCacheable(key, this::get);
		}
	}
}
