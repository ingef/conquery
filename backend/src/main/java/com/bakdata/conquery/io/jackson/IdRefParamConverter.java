package com.bakdata.conquery.io.jackson;

import javax.ws.rs.ext.ParamConverter;

import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.IdResolveContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdRefParamConverter<ID extends IId<VALUE>, VALUE extends Identifiable<ID>> implements ParamConverter<VALUE> {
	private final IId.Parser<ID> idParser;
	@NonNull
	private final IdResolveContext resolveContext;

	@Override
	public VALUE fromString(String value) {
		final ID id = idParser.parse(value);

		final CentralRegistry registry;

		if (id instanceof NamespacedId) {
			registry = resolveContext.findRegistry(((NamespacedId) id).getDataset());
		}
		else {
			registry = resolveContext.getMetaRegistry();
		}

		return registry.resolve(id);
	}

	@Override
	public String toString(VALUE value) {
		return value.getId().toString();
	}
}
