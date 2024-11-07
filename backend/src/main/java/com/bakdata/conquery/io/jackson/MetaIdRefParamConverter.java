package com.bakdata.conquery.io.jackson;

import jakarta.ws.rs.ext.ParamConverter;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetaIdRefParamConverter<ID extends Id<VALUE> & MetaId, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	@NonNull
	private final MetaStorage storage;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return (VALUE) id.get(storage);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
