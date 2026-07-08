package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BigMultiSelectFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "BIG_MULTI_SELECT";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		return filter(context, payload, "BIG_MULTI_SELECT", null, null, true, List.of(requiredColumn(context, payload)));
	}
}
