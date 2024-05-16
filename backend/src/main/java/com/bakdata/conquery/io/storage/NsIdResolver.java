package com.bakdata.conquery.io.storage;

import java.util.NoSuchElementException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface NsIdResolver extends Injectable {

	/**
	 * @param id
	 * @param <ID>
	 * @param <VALUE>
	 * @return
	 */
	<ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE get(ID id);

	static NsIdResolver get(DeserializationContext ctxt) throws JsonMappingException {
		NsIdResolver nsIdResolver = (NsIdResolver) ctxt
				.findInjectableValue(NsIdResolver.class.getName(), null, null);
		if (nsIdResolver == null) {
			throw new NoSuchElementException("Could not find injected namespaces");
		}
		return nsIdResolver;
	}
}
