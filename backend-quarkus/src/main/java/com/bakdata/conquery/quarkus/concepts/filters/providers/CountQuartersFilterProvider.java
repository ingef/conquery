package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountQuartersFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountQuartersFilterProvider extends AbstractFilterProvider<CountQuartersFilterDefinition> {
	public CountQuartersFilterProvider() {
		super(CountQuartersFilterDefinition.class, IntegerRangeFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CountQuartersFilterDefinition payload) {
		return filter(context, payload, IntegerRangeFilterValue.class, 1, null, false, List.of(requiredColumn(context, payload)));
	}
}
