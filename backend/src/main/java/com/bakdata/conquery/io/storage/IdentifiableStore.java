package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.util.functions.ThrowingConsumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Store for Identifiable values.
 * <p>
 * The {@link ThrowingConsumer}s can be used to reflect/model dependencies of the identifiable values inside the store. For example {@link com.bakdata.conquery.models.datasets.concepts.Concept} holds multiple {@link com.bakdata.conquery.models.datasets.concepts.Connector}s where a deletion of a concept requires the deletion of the Conncetors as well. {@link NamespacedStorage} is the main user of those two methods and should be looked at if desired.
 */
@Accessors(fluent = true)
@Setter
@Getter
public class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<Id<VALUE>, VALUE> {

	// TODO: 09.01.2020 fk: Consider making these part of a class that is passed on creation instead so they are less loosely bound.
	@NonNull
	protected ThrowingConsumer<VALUE> onAdd = (v) -> {
	};

	@NonNull
	protected ThrowingConsumer<VALUE> onRemove = (v) -> {
	};

	public IdentifiableStore(Store<Id<VALUE>, VALUE> store) {
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
