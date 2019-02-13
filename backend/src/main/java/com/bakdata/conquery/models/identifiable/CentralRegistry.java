package com.bakdata.conquery.models.identifiable;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.NoArgsConstructor;

@SuppressWarnings({"rawtypes", "unchecked"}) @NoArgsConstructor
public class CentralRegistry implements Injectable {
	
	private final IdMap map = new IdMap<>();
	
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

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(CentralRegistry.class, this);
	}

	public static CentralRegistry get(DeserializationContext ctxt) throws JsonMappingException {
		CentralRegistry result = (CentralRegistry) ctxt.findInjectableValue(CentralRegistry.class.getName(), null, null);
		if(result == null) {
			NamespaceCollection alternative = (NamespaceCollection)ctxt.findInjectableValue(NamespaceCollection.class.getName(), null, null);
			if(alternative == null) {
				throw new NoSuchElementException("Could not find injected central registry");
			}
			else {
				return alternative.getMetaRegistry();
			}
		}
		else {
			return result;
		}
	}
}
