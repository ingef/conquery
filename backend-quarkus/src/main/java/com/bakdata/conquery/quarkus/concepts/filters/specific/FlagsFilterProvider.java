package com.bakdata.conquery.quarkus.concepts.filters.specific;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FlagsFilterProvider extends AbstractBuiltinFilterProvider {
	@Override
	public String type() {
		return "FLAGS";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, CommonFilterPayload payload) {
		return filter(context, payload, "MULTI_SELECT", null, null, false, flagColumns(context, payload.flags()));
	}
}
