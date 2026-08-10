package com.bakdata.conquery.quarkus.concepts.filters.providers;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.DurationSumFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DurationSumFilterProvider extends AbstractFilterProvider<DurationSumFilterDefinition> {
	public DurationSumFilterProvider() {
		super(DurationSumFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, DurationSumFilterDefinition payload) {
		ColumnId column = requiredColumn(context, payload);
		return filter(context, payload, IntegerRangeFilterValue.class, 0, null, false, columns(column, optionalColumns(context, payload.getDistinctBy())));
	}
}
