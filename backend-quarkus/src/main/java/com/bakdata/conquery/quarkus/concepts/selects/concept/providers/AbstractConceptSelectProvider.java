package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import java.util.Optional;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectDefinitionProvider;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.AbstractConceptSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ConceptSelectId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;

abstract class AbstractConceptSelectProvider<T extends AbstractConceptSelectDefinition> implements ConceptSelectDefinitionProvider<T> {

	private final Class<T> modelType;

	protected AbstractConceptSelectProvider(Class<T> modelType) {
		this.modelType = modelType;
	}

	@Override
	public Class<T> modelType() {
		return modelType;
	}

	protected DatasetCatalogRepository.ConceptSelect select(ConceptSelectConversionContext context, T payload, DatasetCatalogRepository.SelectResultType resultType) {
		String name = context.idPartFromPreferredOrFallback(payload.getName(), payload.getLabel(), type());
		String label = firstNonBlank(payload.getLabel(), payload.getName()).orElse(name);
		ConceptSelectId id = context.selectId(name);
		return new DatasetCatalogRepository.ConceptSelect(id, label, payload.getDescription(), payload.isDefault(), type(), resultType);
	}

	protected DatasetCatalogRepository.SelectResultType primitive(String type) {
		return DatasetCatalogRepository.SelectResultType.primitive(type);
	}

	protected DatasetCatalogRepository.SelectResultType list(String elementType) {
		return DatasetCatalogRepository.SelectResultType.list(primitive(elementType));
	}

	private Optional<String> firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return Optional.of(value.trim());
			}
		}
		return Optional.empty();
	}
}
