package com.bakdata.conquery.io.storage;

import java.util.NoSuchElementException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface for classes that can resolve an {@link NamespacedId} to a concrete object.
 */
public interface NsIdResolver extends Injectable {

	/**
	 * Return the object identified by the given id
	 * @param id reference to an object
	 * @param <ID> id type
	 * @param <VALUE> value type of the resolved object
	 * @return the object or null if no object could be resolved. If the id type is not supported
	 * throws a IllegalArgumentException
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
