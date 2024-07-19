package com.bakdata.conquery.io.jackson;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import jakarta.ws.rs.ext.ParamConverter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NamespacedIdRefParamConverter<ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {

	private final IdUtil.Parser<ID> idParser;
	private final DatasetRegistry datasetRegistry;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		return (VALUE) datasetRegistry.get(id);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
