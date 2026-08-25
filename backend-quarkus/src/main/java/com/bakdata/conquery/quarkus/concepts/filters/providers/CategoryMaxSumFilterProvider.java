package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterResult;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CategoryMaxSumFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryMaxSumFilterProvider extends AbstractFilterProvider<CategoryMaxSumFilterDefinition> {

	public CategoryMaxSumFilterProvider() {
		super(CategoryMaxSumFilterDefinition.class, IntegerRangeFilterValue.class, MoneyRangeFilterValue.class, RealRangeFilterValue.class);
	}


	@Override
	public FilterResult convert(FilterConversionContext context, CategoryMaxSumFilterDefinition payload) {
		List<ColumnDescriptor> required = new ArrayList<>();
		ColumnDescriptor valueColumn = context.requireColumn(payload.getValueColumn());
		required.add(valueColumn);
		required.add(context.requireColumn(payload.getCategoryColumn()));
		return filter(context, payload, numericRangeValueType(valueColumn), null, null, false, required);
	}
}
