package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdResolvingException;
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


	/**
	 * Almost identical to {@link NsIdResolver#get(Id)}, but throws an {@link IdResolvingException} if no object could be resolved.
	 * @return the object or throws an {@link IdResolvingException} if the Object could not be resolved.
	 */
	default <ID extends Id<?> & NamespacedId, VALUE> VALUE resolve(ID id) {
		try {
			VALUE o = get(id);
			if (o == null) {
				throw new IdResolvingException(id);
			}
			return o;
		}
		catch (Exception e) {
			throw new IdResolvingException(id, e);
		}
	}

	static NsIdResolver getResolver(DeserializationContext ctxt) throws JsonMappingException {
		return (NsIdResolver) ctxt
				.findInjectableValue(NsIdResolver.class.getName(), null, null);
	}
}
