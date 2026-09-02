package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MultiSelectFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MultiSelectFilterValueProvider extends AbstractFilterValueProvider<MultiSelectFilterValue> {
	public MultiSelectFilterValueProvider() {
		super(MultiSelectFilterValue.class);
	}

}
