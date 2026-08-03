package com.bakdata.conquery.quarkus.concepts.filters;

import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface FilterDefinitionProvider<T extends FilterDefinition> extends PolymorphicModelTypeProvider<FilterDefinition, T> {

	String type();

	Class<T> payloadType();

	Set<Class<? extends FilterValue>> acceptedValueTypes();

	DatasetCatalogRepository.Filter convert(FilterConversionContext context, T payload);

	@Override
	default Class<FilterDefinition> baseType() {
		return FilterDefinition.class;
	}

	@Override
	default String typeId() {
		return type();
	}

	@Override
	default Class<T> modelType() {
		return payloadType();
	}
}
