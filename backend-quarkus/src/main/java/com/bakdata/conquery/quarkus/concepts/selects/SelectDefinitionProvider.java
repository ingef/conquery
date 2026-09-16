package com.bakdata.conquery.quarkus.concepts.selects;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public interface SelectDefinitionProvider<T extends SelectDefinition> extends PolymorphicModelTypeProvider<SelectDefinition, T> {

	DatasetCatalogRepository.Select convert(SelectConversionContext context, T payload);

	default String type() {
		return typeId();
	}
}
