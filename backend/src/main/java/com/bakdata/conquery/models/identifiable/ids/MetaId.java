package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Setter;

/**
 * Marker interface for Ids that are resolvable in a {@link com.bakdata.conquery.io.storage.MetaStorage}
 */
public abstract non-sealed class MetaId<TYPE> extends Id<TYPE, MetaStorage> {

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@JsonIgnore
	private MetaStorage domain;

	protected MetaStorage getDomain() {
		return domain;
	}
}
