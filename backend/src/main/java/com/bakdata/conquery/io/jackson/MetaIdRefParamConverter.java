package com.bakdata.conquery.io.jackson;

import javax.ws.rs.ext.ParamConverter;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetaIdRefParamConverter<ID extends Id<VALUE>, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
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
