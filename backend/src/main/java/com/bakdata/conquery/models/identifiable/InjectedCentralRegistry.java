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
	private final CentralRegistry delegate;

	@Override
	protected <T extends Identifiable<?>> T get(IId<T> name) {
		final Identifiable<?> result = injections.get(name);

		if(result != null){
			return ((T) result);
		}

		return delegate.get(name);
	}
}
