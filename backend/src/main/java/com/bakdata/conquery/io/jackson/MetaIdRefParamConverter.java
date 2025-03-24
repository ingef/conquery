package com.bakdata.conquery.io.jackson;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.MetaId;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.ParamConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MetaIdRefParamConverter<ID extends Id<VALUE> & MetaId, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	@NonNull
	private final MetaStorage storage;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		final Identifiable<?> identifiable = id.get(storage);
		if (identifiable == null) {
			final String msg = "Unable to resolve id: %s".formatted(id);
			log.warn(msg);
			throw new NotFoundException(msg);
		}
		return (VALUE) identifiable;
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
