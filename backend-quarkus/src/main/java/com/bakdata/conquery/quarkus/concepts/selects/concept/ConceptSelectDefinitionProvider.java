package com.bakdata.conquery.quarkus.concepts.selects.concept;

import com.bakdata.conquery.quarkus.models.PolymorphicModelTypeProvider;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

public interface ConceptSelectDefinitionProvider<T extends ConceptSelectDefinition> extends PolymorphicModelTypeProvider<ConceptSelectDefinition, T> {

	String type();

	Class<T> payloadType();

	DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, T payload);

	@Override
	default Class<ConceptSelectDefinition> baseType() {
		return ConceptSelectDefinition.class;
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
