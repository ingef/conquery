package com.bakdata.conquery.quarkus.concepts.filters.values;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface FilterValueProvider<T extends FilterValue> extends PolymorphicModelTypeProvider<FilterValue, T> {

	String type();

	Class<T> payloadType();

	@Override
	default Class<FilterValue> baseType() {
		return FilterValue.class;
	}

	@Override
	default String typeId() {
		return type();
	}

	@Override
	default Class<T> modelType() {
		return payloadType();
	}
}
