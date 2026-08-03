package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueProvider;

abstract class AbstractFilterValueProvider<T extends FilterValue> implements FilterValueProvider<T> {

	private final Class<T> payloadType;

	protected AbstractFilterValueProvider(Class<T> payloadType) {
		this.payloadType = payloadType;
	}

	@Override
	public Class<T> payloadType() {
		return payloadType;
	}
}
