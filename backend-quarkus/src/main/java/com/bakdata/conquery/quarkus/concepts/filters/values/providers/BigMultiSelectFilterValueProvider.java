package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.BigMultiSelectFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BigMultiSelectFilterValueProvider extends AbstractFilterValueProvider<BigMultiSelectFilterValue> {
	public BigMultiSelectFilterValueProvider() {
		super(BigMultiSelectFilterValue.class);
	}

}
