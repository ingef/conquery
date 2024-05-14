package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;

/**
 * Registered items are directly referenced
 */
public class DirectIdentifiableStore<VALUE extends Identifiable<?>> extends IdentifiableStore<VALUE> {

	public DirectIdentifiableStore(Store<Id<VALUE>, VALUE> store) {
		super(store);
	}

	@Override
	protected Id<VALUE> extractKey(VALUE value) {
		return (Id<VALUE>) value.getId();
	}

	@Override
	protected void removed(VALUE value) {
		try {
			if (value == null) {
				return;
			}

			onRemove.accept(value);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to remove " + value, e);
		}
	}

	@Override
	protected void added(VALUE value) {
		try {
			if (value == null) {
				return;
			}

			onAdd.accept(value);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to add " + value, e);
		}
	}

	@Override
	protected void updated(VALUE value) {
		try {
			if (value == null) {
				return;
			}
			final VALUE old = store.get((Id<VALUE>) value.getId());

			if (old != null) {
				onRemove.accept(old);
			}

			onAdd.accept(value);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to add " + value, e);
		}
	}
}
