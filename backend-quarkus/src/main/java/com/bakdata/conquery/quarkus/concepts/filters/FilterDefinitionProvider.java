package com.bakdata.conquery.quarkus.concepts.filters;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public interface FilterDefinitionProvider<T> {

	String type();

	Class<T> payloadType();

	DatasetCatalogRepository.Filter convert(FilterConversionContext context, T payload);
}
