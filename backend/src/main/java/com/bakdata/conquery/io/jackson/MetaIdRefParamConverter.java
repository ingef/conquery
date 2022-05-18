package com.bakdata.conquery.io.jackson;

import javax.ws.rs.ext.ParamConverter;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetaIdRefParamConverter<ID extends AId<VALUE>, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final AId.Parser<ID> idParser;
	@NonNull
	private final CentralRegistry registry;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return registry.resolve(id);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
