package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.util.functions.ThrowingConsumer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent=true) @Setter @Getter
public class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<IId<VALUE>, VALUE> {

	private final CentralRegistry centralRegistry;
	protected ThrowingConsumer<VALUE> onAdd;
	protected ThrowingConsumer<VALUE> onRemove;
	
	public IdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store) {
		this(centralRegistry, store, new SingletonNamespaceCollection(centralRegistry));
	}
	
	public IdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store, Injectable... injectables) {
		super(store);
		for(Injectable injectable : injectables) {
			store.inject(injectable);
		}
		store.inject(centralRegistry);
		this.centralRegistry = centralRegistry;
	}

	@Override
	protected IId<VALUE> extractKey(VALUE value) {
		return (IId<VALUE>)value.getId();
	}
	
	@Override
	protected void removed(VALUE value) {
		try {
			if(value != null) {
				if(onRemove != null) {
					onRemove.accept(value);
				}
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
				centralRegistry.register(value);
				if(onAdd != null) {
					onAdd.accept(value);
				}
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value, e);
		}
	}
}
