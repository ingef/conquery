package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValueProvider;

abstract class AbstractFilterValueProvider<T extends FilterValue> implements FilterValueProvider<T> {

	private final Class<T> modelType;

	protected AbstractFilterValueProvider(Class<T> modelType) {
		this.modelType = modelType;
	}

	@Override
	public Class<T> modelType() {
		return modelType;
	}
}
