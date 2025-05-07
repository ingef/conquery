package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public abstract class MetaIdentifiable<ID extends MetaId<?>> extends IdentifiableImpl<ID> {

	@NonNull
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	private MetaStorage metaStorage;

	@Override
	protected void injectDomain(MetaId id) {
		id.setDomain(metaStorage);
	}
}
