package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountQuartersFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountQuartersFilterProvider extends AbstractFilterProvider<CountQuartersFilterDefinition> {
	public CountQuartersFilterProvider() {
		super(CountQuartersFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, CountQuartersFilterDefinition payload) {
		return filter(context, payload, IntegerRangeFilterValue.class, 1, null, false, List.of(requiredColumn(context, payload)));
	}
}
