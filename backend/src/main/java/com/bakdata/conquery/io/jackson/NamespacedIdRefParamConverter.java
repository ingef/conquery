package com.bakdata.conquery.io.jackson;

import jakarta.ws.rs.ext.ParamConverter;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NamespacedIdRefParamConverter<ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	@NonNull
	private final DatasetRegistry registry;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return (VALUE) id.get(registry.getStorage(id.getDataset()));
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
