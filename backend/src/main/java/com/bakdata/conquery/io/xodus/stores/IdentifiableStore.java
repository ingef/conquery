package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.functions.ThrowingConsumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Store for Identifiable values. Directly delegates all changes to the supplied {@link CentralRegistry}.
 *
 * The {@link ThrowingConsumer}s can be used to reflect/model dependencies of the identifiable values inside the store. For example {@link com.bakdata.conquery.models.concepts.Concept} holds multiple {@link com.bakdata.conquery.models.concepts.Connector}s where a deletion of a concept requires the deletion of the Conncetors as well. {@link com.bakdata.conquery.io.xodus.NamespacedStorageImpl} is the main user of those two methods and should be looked at if desired.
 */
@Accessors(fluent=true) @Setter @Getter
public class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<IId<VALUE>, VALUE> {

	private final CentralRegistry centralRegistry;

	// TODO: 09.01.2020 fk: Consider making these part of a class that is passed on creation instead so they are less loosely bound.
	@NonNull
	protected ThrowingConsumer<VALUE> onAdd = (v) -> {};

	@NonNull
	protected ThrowingConsumer<VALUE> onRemove = (v) -> {};

	public IdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store) {
		super(store);

		this.centralRegistry = centralRegistry;
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
