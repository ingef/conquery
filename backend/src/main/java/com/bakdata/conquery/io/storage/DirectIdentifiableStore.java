package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.functions.ThrowingConsumer;

import java.util.Optional;

/**
 * Registered items are directly referenced. Compare to {@link IdentifiableCachedStore}
 */
public class DirectIdentifiableStore<VALUE extends Identifiable<?>> extends IdentifiableStore<VALUE> {

	public DirectIdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store) {
		super(store, centralRegistry);
	}

	@Override
	protected IId<VALUE> extractKey(VALUE value) {
		return (IId<VALUE>)value.getId();
	}
	
	@Override
	protected void removed(VALUE value) {
		try {
			if (value == null) {
				return;
			}

			onRemove.accept(value);
			centralRegistry.remove(value);
		} catch(Exception e) {
			throw new RuntimeException("Failed to remove "+value, e);
		}
	}

	@Override
	protected void added(VALUE value) {
		try {
			if (value == null) {
				return;
			}

			centralRegistry.register(value);
			onAdd.accept(value);
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}

	@Override
	protected void updated(VALUE value) {
		try {
			if (value == null) {
				return;
			}
			final Optional<? extends Identifiable<?>> old = centralRegistry.getOptional(value.getId());

			if (old.isPresent()) {
				onRemove.accept((VALUE) old.get());
			}

			centralRegistry.update(value);
			onAdd.accept(value);
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
}
