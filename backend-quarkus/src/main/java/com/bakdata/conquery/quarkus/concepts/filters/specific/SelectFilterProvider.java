package com.bakdata.conquery.quarkus.concepts.filters.specific;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SelectFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "SELECT";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		String frontendType = options(payload).isEmpty() ? "BIG_MULTI_SELECT" : "MULTI_SELECT";
		return filter(context, payload, frontendType, null, null, options(payload).isEmpty(), List.of(requiredColumn(context, payload)));
	}
}
