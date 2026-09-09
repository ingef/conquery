package com.bakdata.conquery.quarkus.concepts.filters.values.providers;

import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MoneyRangeFilterValueProvider extends AbstractFilterValueProvider<MoneyRangeFilterValue> {
	public MoneyRangeFilterValueProvider() {
		super(MoneyRangeFilterValue.class);
	}

}
