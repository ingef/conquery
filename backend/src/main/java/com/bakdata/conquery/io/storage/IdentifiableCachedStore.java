package com.bakdata.conquery.io.storage;

import java.util.Optional;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Registers accessors of values instead of the value itself to the central registry.
 * Might be useful if the object are very large and should only be loaded on demand.
 */
@Accessors(fluent=true) @Setter @Getter
public class IdentifiableCachedStore<VALUE extends Identifiable<?>> extends IdentifiableStore<VALUE> {

	public IdentifiableCachedStore(CentralRegistry centralRegistry, Store<Id<VALUE>, VALUE> store) {
		super(store, centralRegistry);
	}

	@Override
	protected Id<VALUE> extractKey(VALUE value) {
		return (Id<VALUE>) value.getId();
	}
	
	@Override
	protected void removed(VALUE value) {
		try {
			if(value != null) {
				onRemove.accept(value);
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
				final Id<VALUE> key = extractKey(value);
				centralRegistry.registerCacheable(key, this::get);
				onAdd.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}

	@Override
	protected void updated(VALUE value) {
		try {
			if(value != null) {
				final Id<VALUE> key = extractKey(value);
				final Optional<Identifiable> oldOpt = centralRegistry.updateCacheable(key, this::get);
				if (oldOpt.isPresent()) {
					final VALUE old = (VALUE) oldOpt.get();
					onRemove.accept(old);
				}
				onAdd.accept(value);
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
	
	@Override
	public void loadData() {
		store.loadData();
		for (Id<VALUE> key : getAllKeys()) {
			centralRegistry.registerCacheable(key, this::get);
		}
	}
}
