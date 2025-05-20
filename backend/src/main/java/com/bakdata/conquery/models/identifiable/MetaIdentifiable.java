package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public non-sealed abstract class MetaIdentifiable<ID extends MetaId<?>> extends IdentifiableImpl<ID, MetaStorage> {

	@NonNull
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	private MetaStorage metaStorage;

	@Override
	public final MetaStorage getDomain() {
		return getMetaStorage();
	}

}
