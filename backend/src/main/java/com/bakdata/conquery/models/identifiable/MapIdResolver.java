package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.NonNull;

/**
 * Helper class to wrap a map into a resolver.
 *
 * @param injections This map is intentionally NOT an IdMap as it allows wiring up mismatched ids.
 */
public record MapIdResolver(@NonNull Map<Id<?>, Identifiable<?>> injections) implements NsIdResolver {
	@Override
	public <ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id) {
		return (VALUE) injections.get(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}
}
