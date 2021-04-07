package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class InjectedCentralRegistry extends CentralRegistry{
	/**
	 * This map is intentionally NOT an IdMap as it allows wiring up mismatched ids.
	 */
	@NonNull
	private final Map<IId<?>, Identifiable<?>> injections;

	@Override
	protected <T extends Identifiable<?>> T get(IId<T> name) {
		return (T) injections.get(name);
	}
}
