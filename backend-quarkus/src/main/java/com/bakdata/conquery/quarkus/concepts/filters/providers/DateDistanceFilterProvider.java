package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.DateDistanceFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DateDistanceFilterProvider extends AbstractFilterProvider<DateDistanceFilterDefinition> {
	public DateDistanceFilterProvider() {
		super(DateDistanceFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, DateDistanceFilterDefinition payload) {
		return filter(context, payload, IntegerRangeFilterValue.class, null, null, false, List.of(requiredColumn(context, payload)));
	}
}
