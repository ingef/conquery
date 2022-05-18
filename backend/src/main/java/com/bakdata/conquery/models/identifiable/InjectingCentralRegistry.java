package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.models.identifiable.ids.AId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Central Registry used to wire up incoming ids with already established ids.
 */
@RequiredArgsConstructor
@Data
public class InjectingCentralRegistry extends CentralRegistry{
	/**
	 * This map is intentionally NOT an IdMap as it allows wiring up mismatched ids.
	 */
	@NonNull
	private final Map<AId<?>, Identifiable<?>> injections;

	@Override
	protected <T extends Identifiable<?>> T get(AId<T> name) {
		return (T) injections.get(name);
	}
}
