package com.bakdata.conquery.quarkus.concepts.filters;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;

public interface FilterDefinitionProvider<T extends FilterDefinition> extends PolymorphicModelTypeProvider<FilterDefinition, T> {

	String type();

	Class<T> payloadType();

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
