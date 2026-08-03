package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RealRangeFilterValueProvider extends AbstractFilterValueProvider<RealRangeFilterValue> {
	public RealRangeFilterValueProvider() {
		super(RealRangeFilterValue.class);
	}

	@Override
	public String type() {
		return "REAL_RANGE";
	}
}
