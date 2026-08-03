package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IntegerRangeFilterValueProvider extends AbstractFilterValueProvider<IntegerRangeFilterValue> {
	public IntegerRangeFilterValueProvider() {
		super(IntegerRangeFilterValue.class);
	}

	@Override
	public String type() {
		return "INTEGER_RANGE";
	}
}
