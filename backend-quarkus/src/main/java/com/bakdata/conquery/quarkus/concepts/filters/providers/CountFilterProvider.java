package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext.Column;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountFilterProvider extends AbstractFilterProvider<CountFilterDefinition> {
	public CountFilterProvider() {
		super(CountFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, CountFilterDefinition payload) {
		Column column = requiredColumn(context, payload);
		return filter(context, payload, IntegerRangeFilterValue.class, 1, null, false, columns(column, optionalColumns(context, payload.getDistinctByColumn())));
	}
}
