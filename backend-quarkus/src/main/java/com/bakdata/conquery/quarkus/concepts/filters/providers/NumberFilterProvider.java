package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.NumberFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NumberFilterProvider extends AbstractFilterProvider<NumberFilterDefinition> {
	public NumberFilterProvider() {
		super(NumberFilterDefinition.class, IntegerRangeFilterValue.class, MoneyRangeFilterValue.class, RealRangeFilterValue.class);
	}
	@Override
	public String type() {
		return "NUMBER";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, NumberFilterDefinition payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, numericRangeValueType(context, column), null, null, false, List.of(column));
	}
}
