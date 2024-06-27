package com.bakdata.conquery.io.jackson;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import jakarta.ws.rs.ext.ParamConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This converter converts MetaIds to the concrete objects
 *
 * @param <ID>
 * @param <VALUE>
 */
@RequiredArgsConstructor
public class MetaIdRefParamConverter<ID extends Id<VALUE>, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	@NonNull
	private final MetaStorage storage;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return storage.get(id);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
