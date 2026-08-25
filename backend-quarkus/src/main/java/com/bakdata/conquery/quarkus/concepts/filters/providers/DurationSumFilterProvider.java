package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.DurationSumFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DurationSumFilterProvider extends AbstractFilterProvider<DurationSumFilterDefinition> {
	public DurationSumFilterProvider() {
		super(DurationSumFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, DurationSumFilterDefinition payload) {
		ColumnDescriptor column = requiredColumn(context, payload);
		return filter(context, payload, IntegerRangeFilterValue.class, 0, null, false, columns(column, optionalColumns(context, payload.getDistinctBy())));
	}
}
