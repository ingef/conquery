package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.functions.ThrowingConsumer;

/**
 * Store for Identifiable values. Directly delegates all changes to the supplied {@link CentralRegistry}.
 *
 * The {@link ThrowingConsumer}s can be used to reflect/model dependencies of the identifiable values inside the store. For example {@link com.bakdata.conquery.models.concepts.Concept} holds multiple {@link com.bakdata.conquery.models.concepts.Connector}s where a deletion of a concept requires the deletion of the Conncetors as well. {@link NamespacedStorage} is the main user of those two methods and should be looked at if desired.
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
}
