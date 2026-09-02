package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.QuartersInYearFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuartersInYearFilterProvider extends AbstractFilterProvider<QuartersInYearFilterDefinition> {
	public QuartersInYearFilterProvider() {
		super(QuartersInYearFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, QuartersInYearFilterDefinition payload) {
		return filter(context, payload, IntegerRangeFilterValue.class, 1, 4, false, List.of(requiredColumn(context, payload)));
	}
}
