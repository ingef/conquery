package com.bakdata.conquery.quarkus.concepts.selects;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public interface SelectDefinitionProvider<T extends SelectDefinition> extends PolymorphicModelTypeProvider<SelectDefinition, T> {

	String type();

	Class<T> payloadType();

	DatasetCatalogRepository.Select convert(SelectConversionContext context, T payload);

	@Override
	default Class<SelectDefinition> baseType() {
		return SelectDefinition.class;
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
