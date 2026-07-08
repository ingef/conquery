package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CountQuartersFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "COUNT_QUARTERS";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		return filter(context, payload, "INTEGER_RANGE", 1, null, false, List.of(requiredColumn(context, payload)));
	}
}
