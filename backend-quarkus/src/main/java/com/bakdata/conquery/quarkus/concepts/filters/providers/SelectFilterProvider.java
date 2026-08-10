package com.bakdata.conquery.quarkus.concepts.filters.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.definitions.MultiSelectFilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.BigMultiSelectFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MultiSelectFilterValue;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SelectFilterProvider extends AbstractFilterProvider<MultiSelectFilterDefinition> {
	public SelectFilterProvider() {
		super(MultiSelectFilterDefinition.class, MultiSelectFilterValue.class, BigMultiSelectFilterValue.class);
	}

	@Override
	public DatasetCatalogRepository.Filter convert(FilterConversionContext context, MultiSelectFilterDefinition payload) {
		boolean hasOptions = !options(payload).isEmpty();
		Class<? extends FilterValue> valueType = hasOptions
				? MultiSelectFilterValue.class
				: BigMultiSelectFilterValue.class;
		return filter(context, payload, valueType, null, null, !hasOptions, List.of(requiredColumn(context, payload)));
	}
}
