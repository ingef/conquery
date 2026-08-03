package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IntegerFilterValueProvider extends AbstractFilterValueProvider<IntegerFilterValue> {
	public IntegerFilterValueProvider() {
		super(IntegerFilterValue.class);
	}

	@Override
	public String type() {
		return "INTEGER";
	}
}
