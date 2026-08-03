package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RealFilterValueProvider extends AbstractFilterValueProvider<RealFilterValue> {
	public RealFilterValueProvider() {
		super(RealFilterValue.class);
	}

	@Override
	public String type() {
		return "REAL";
	}
}
