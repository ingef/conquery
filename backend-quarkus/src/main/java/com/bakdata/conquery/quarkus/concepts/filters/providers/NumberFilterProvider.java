package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.models.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.NumberFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NumberFilterProvider extends AbstractFilterProvider<NumberFilterDefinition> {
	public NumberFilterProvider() {
		super(NumberFilterDefinition.class, IntegerRangeFilterValue.class, MoneyRangeFilterValue.class, RealRangeFilterValue.class);
	}

	@Override
	public FilterResult convert(FilterConversionContext context, NumberFilterDefinition payload) {
		ColumnDescriptor column = requiredColumn(context, payload);
		return filter(context, payload, numericRangeValueType(column), null, null, false, List.of(column));
	}
}
