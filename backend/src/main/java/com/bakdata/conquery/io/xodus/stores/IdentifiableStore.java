package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;

public class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<IId<VALUE>, VALUE> {

	private final CentralRegistry centralRegistry;
	
	public IdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store) {
		this(centralRegistry, store, new SingletonNamespaceCollection(centralRegistry));
	}
	
	public IdentifiableStore(CentralRegistry centralRegistry, Store<IId<VALUE>, VALUE> store, Injectable... injectables) {
		super(store);
		for(Injectable injectable : injectables) {
			store.inject(injectable);
		}
		this.centralRegistry = centralRegistry;
		fillCache();
	}

	@Override
	protected IId<VALUE> extractKey(VALUE value) {
		return (IId<VALUE>)value.getId();
	}
	
	@Override
	public void add(VALUE value) throws JSONException {
		super.add(value);
		try {
			centralRegistry.register(value);
			addToRegistry(centralRegistry, value);
		} catch(Exception e) {
			throw new RuntimeException("Failed to add "+value+" to the registry", e);
		}
	}
	
	@Override
	public void update(VALUE value) throws JSONException {
		super.update(value);
		centralRegistry.remove(value.getId());
		synchronized (centralRegistry) {
			removeFromRegistry(centralRegistry, value);
			try {
				centralRegistry.register(value);
				addToRegistry(centralRegistry, value);
			} catch(Exception e) {
				throw new RuntimeException("Failed to add "+value+" to the registry", e);
			}
		}
	}
	
	@Override
	public void remove(IId<VALUE> key) {
		super.remove(key);
		centralRegistry.remove(key);
	}
	
	@Override
	public void fillCache() {
		super.fillCache();
		for(VALUE value:getAll()) {
			try {
				centralRegistry.register(value);
				addToRegistry(centralRegistry, value);
			} catch(Exception e) {
				throw new RuntimeException("Failed to add "+value+" to the registry", e);
			}
		}
	}

	protected void addToRegistry(CentralRegistry centralRegistry, VALUE value) throws Exception {}
	protected void removeFromRegistry(CentralRegistry centralRegistry, VALUE value) {}
}
