package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.Injectable;
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
	<ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id);

	static NsIdResolver getResolver(DeserializationContext ctxt) throws JsonMappingException {
		return (NsIdResolver) ctxt
				.findInjectableValue(NsIdResolver.class.getName(), null, null);
	}
}
