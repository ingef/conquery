package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.NonNull;

/**
 * Central Registry used to wire up incoming ids with already established ids.
 *
 * @param injections This map is intentionally NOT an IdMap as it allows wiring up mismatched ids.
 */
public record MapNsIdResolver(@NonNull Map<Id<?>, Identifiable<?>> injections) implements NsIdResolver {
	@Override
	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE get(ID id) {
		return (VALUE) injections.get(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}
}
