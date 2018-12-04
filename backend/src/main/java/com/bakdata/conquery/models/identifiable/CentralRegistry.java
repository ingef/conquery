package com.bakdata.conquery.models.identifiable;

import java.util.Optional;

import com.bakdata.conquery.models.identifiable.ids.IId;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CentralRegistry {
	
	private final IdMap map = new IdMap<>();
	
	public CentralRegistry() {
		map.initMap();
	}
	
	public void register(Identifiable<?> ident) {
		map.add(ident);
	}
	
	public <T extends Identifiable<?>> T resolve(IId<T> name) {
		return (T) map.getOrFail(name);
	}

	public <T extends Identifiable<?>> Optional<T> getOptional(IId<T> name) {
		return map.getOptional(name);
	}

	public void remove(IId<?> id) {
		map.remove(id);
	}
	
	public void remove(Identifiable<?> ident) {
		remove(ident.getId());
	}
}
