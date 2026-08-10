package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountFilterProvider extends AbstractFilterProvider<CountFilterDefinition> {
	public CountFilterProvider() {
		super(CountFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CountFilterDefinition payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, IntegerRangeFilterValue.class, 1, null, false, columns(column, optionalColumns(context, payload.getDistinctByColumn())));
	}
}
