package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.BigMultiSelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.BigMultiSelectFilterValue;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BigMultiSelectFilterProvider extends AbstractFilterProvider<BigMultiSelectFilterDefinition> {
	public BigMultiSelectFilterProvider() {
		super(BigMultiSelectFilterDefinition.class, BigMultiSelectFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, BigMultiSelectFilterDefinition payload) {
		return filter(context, payload, BigMultiSelectFilterValue.class, null, null, true, List.of(requiredColumn(context, payload)));
	}
}
