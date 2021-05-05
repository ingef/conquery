package com.bakdata.conquery.models.identifiable;

import java.util.Map;

import com.bakdata.conquery.models.identifiable.ids.IId;
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
	private final Map<IId<?>, Identifiable<?>> injections;

	@Override
	protected <T extends Identifiable<?>> T get(IId<T> name) {
		return (T) injections.get(name);
	}
}
