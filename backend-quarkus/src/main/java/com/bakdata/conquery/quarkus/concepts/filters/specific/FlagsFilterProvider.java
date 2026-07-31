package com.bakdata.conquery.quarkus.concepts.filters.specific;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.FlagsFilterDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FlagsFilterProvider extends AbstractBuiltinFilterProvider<FlagsFilterDefinition> {
	public FlagsFilterProvider() {
		super(FlagsFilterDefinition.class);
	}
	@Override
	public String type() {
		return "FLAGS";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, FlagsFilterDefinition payload) {
		return filter(context, payload, "MULTI_SELECT", null, null, false, flagColumns(context, payload.getFlags()));
	}
}
