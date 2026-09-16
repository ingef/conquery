package com.bakdata.conquery.quarkus.concepts.selects.concept;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public interface ConceptSelectDefinitionProvider<T extends ConceptSelectDefinition> extends PolymorphicModelTypeProvider<ConceptSelectDefinition, T> {

	DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, T payload);

	default String type() {
		return typeId();
	}
}
