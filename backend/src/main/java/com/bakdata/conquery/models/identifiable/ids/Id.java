package com.bakdata.conquery.models.identifiable.ids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.models.identifiable.IdResolvingException;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = IdDeserializer.class)
public sealed abstract class Id<TYPE, DOMAIN> permits NamespacedId, MetaId {

	/**
	 * Holds the cached escaped value.
	 *
	 * @implNote needs to be initialized. Otherwise, SerializationTests fail, because assertj checks ignored types.
	 */
	@JsonIgnore
	private WeakReference<String> escapedId = new WeakReference<>(null);

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	@JsonValue
	public final String toString() {
		final String escaped = escapedId.get();
		if (escaped != null) {
			return escaped;
		}

		String escapedIdString = escapedIdString();
		escapedId = new WeakReference<>(escapedIdString);
		return escapedIdString;
	}

	public final String escapedIdString() {
		List<Object> components = getComponents();
		components.replaceAll(o -> ConqueryEscape.escape(Objects.toString(o)));
		return IdUtil.JOINER.join(components);
	}

	public final List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		collectComponents(components);
		return components;
	}

	public abstract void collectComponents(List<Object> components);

	public final TYPE resolve() {
		try {
			TYPE o = get();
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

	public abstract DOMAIN getDomain();

	public abstract void setDomain(DOMAIN DOMAIN);

	/**
	 * Return the object identified by the given id from the given storage.
	 *
	 * @return the object or null if no object could be resolved.
	 */
	protected abstract TYPE get();

	public final IdResolvingException newIdResolveException() {
		return new IdResolvingException(this);
	}

	public final IdResolvingException newIdResolveException(Exception e) {
		return new IdResolvingException(this, e);
	}

	public abstract void collectIds(Collection<Id<?, ?>> collect);


}
