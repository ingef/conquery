package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Central Registry used to wire up incoming ids with already established ids.
 */
@RequiredArgsConstructor
@Data
public class InjectingCentralRegistry implements NsIdResolver {
	/**
	 * This map is intentionally NOT an IdMap as it allows wiring up mismatched ids.
	 */
	@NonNull
	private final Map<Id<?>, Identifiable<?>> injections;

	@Override
	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE get(ID id) {
		return (VALUE) injections.get(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}
}
