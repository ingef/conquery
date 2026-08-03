package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.MultiSelectFilterDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SelectFilterProvider extends AbstractFilterProvider<MultiSelectFilterDefinition> {
	public SelectFilterProvider() {
		super(MultiSelectFilterDefinition.class);
	}
	@Override
	public String type() {
		return "SELECT";
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, MultiSelectFilterDefinition payload) {
		String frontendType = options(payload).isEmpty() ? "BIG_MULTI_SELECT" : "MULTI_SELECT";
		return filter(context, payload, frontendType, null, null, options(payload).isEmpty(), List.of(requiredColumn(context, payload)));
	}
}
