package com.bakdata.conquery.models.identifiable.ids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.models.identifiable.IdResolvingException;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.NotNull;

@JsonDeserialize(using = IdDeserializer.class)
public sealed interface Id<TYPE, STORAGE> permits NamespacedId, MetaId {


	STORAGE getStorage();
	void setStorage(STORAGE storage);

	@Override
	int hashCode();

	@Override
	boolean equals(Object obj);

	@Override
	@JsonValue
	String toString();

	default String escapedIdString() {
		List<Object> components = getComponents();
		components.replaceAll(o -> ConqueryEscape.escape(Objects.toString(o)));
		return IdUtil.JOINER.join(components);
	}

	default List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		collectComponents(components);
		return components;
	}

	/**
	 * Return the object identified by the given id from the given storage.
	 *
	 * @return the object or null if no object could be resolved. If the id type is not supported
	 * throws a IllegalArgumentException
	 */
	TYPE get(STORAGE storage);


	@NotNull
	default TYPE resolve(STORAGE storage) {
		try {
			TYPE o = get(storage);
			if (o == null) {
				throw newIdResolveException();
			}
			return o;
		}
		catch (IdResolvingException e) {
			throw e;
		}
		catch (Exception e) {
			throw newIdResolveException(e);
		}
	}

	default TYPE resolve() {
		return resolve(getStorage());
	}

	default IdResolvingException newIdResolveException() {
		return new IdResolvingException(this);
	}

	default IdResolvingException newIdResolveException(Exception e) {
		return new IdResolvingException(this, e);
	}

	void collectComponents(List<Object> components);

	void collectIds(Collection<Id<?, ?>> collect);


}
