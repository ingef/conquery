package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountQuartersFilterDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountQuartersFilterProvider extends AbstractBuiltinFilterProvider<CountQuartersFilterDefinition> {
	public CountQuartersFilterProvider() {
		super(CountQuartersFilterDefinition.class);
	}
	@Override
	public String type() {
		return "COUNT_QUARTERS";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CountQuartersFilterDefinition payload) {
		return filter(context, payload, "INTEGER_RANGE", 1, null, false, List.of(requiredColumn(context, payload)));
	}
}
