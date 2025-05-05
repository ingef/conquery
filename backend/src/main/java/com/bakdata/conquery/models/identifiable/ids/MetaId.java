package com.bakdata.conquery.models.identifiable.ids;

import java.lang.ref.WeakReference;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.Setter;

/**
 * Marker interface for Ids that are resolvable in a {@link com.bakdata.conquery.io.storage.MetaStorage}
 */
public abstract non-sealed class MetaId<TYPE> implements Id<TYPE, MetaStorage> {

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter
	@JsonIgnore
	private MetaStorage storage;
	/**
	 * Holds the cached escaped value.
	 *
	 * @implNote needs to be initialized. Otherwise, SerializationTests fail, because assertj checks ignored types.
	 */
	@JsonIgnore
	private WeakReference<String> escapedId = new WeakReference<>(null);


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

}
