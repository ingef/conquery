package com.bakdata.conquery.models.identifiable.ids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.IdResolvingException;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@JsonDeserialize(using = IdDeserializer.class)
@Slf4j
public abstract class Id<TYPE> {

	/**
	 * Holds the cached escaped value.
	 *
	 * @implNote needs to be initialized. Otherwise, SerializationTests fail, because assertj checks ignored types.
	 */
	@JsonIgnore
	private WeakReference<String> escapedId = new WeakReference<>(null);

	/**
	 * Injected by deserializer
	 */
	@JsonIgnore
	@Setter
	@Getter
	private NamespacedStorageProvider namespacedStorageProvider;

	/**
	 * Injected by deserializer for resolving meta Ids
	 */
	@JsonIgnore
	@Setter
	@Getter
	private MetaStorage metaStorage;

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

	private String escapedIdString() {
		List<Object> components = getComponents();
		components.replaceAll(o -> ConqueryEscape.escape(Objects.toString(o)));
		return IdUtil.JOINER.join(components);
	}

	public final List<Object> getComponents() {
		List<Object> components = new ArrayList<>();
		this.collectComponents(components);
		return components;
	}

	public abstract void collectComponents(List<Object> components);

	public final List<String> collectComponents() {
		List<Object> components = getComponents();
		List<String> result = new ArrayList<>(components.size());

		for (Object component : components) {
			result.add(ConqueryEscape.escape(Objects.toString(component)));
		}

		return result;
	}

	public TYPE resolve() {
		if (this instanceof NamespacedId namespacedId) {
			return (TYPE) namespacedId.resolve(getNamespacedStorageProvider().getStorage(namespacedId.getDataset()));
		}
		if (this instanceof MetaId) {
			return metaStorage.resolve((Id<?> & MetaId)this);
		}
		throw new IllegalStateException("Tried to resolve an id that is neither NamespacedId not MetaId: %s".formatted(this));
	}

	public IdResolvingException newIdResolveException() {
		log.warn("Unable to resolve {}", this, new Exception("MARKER"));
		return new IdResolvingException(this);
	}

	public IdResolvingException newIdResolveException(Exception e) {
		return new IdResolvingException(this, e);
	}

	public abstract void collectIds(Collection<? super Id<?>> collect);
}
