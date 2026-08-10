package com.bakdata.conquery.quarkus.concepts.filters.values;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface FilterValueProvider<T extends FilterValue> extends PolymorphicModelTypeProvider<FilterValue, T> {

	default String type() {
		return typeId();
	}
}
