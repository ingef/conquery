package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.ConceptValuesConceptSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConceptValuesConceptSelectProvider extends AbstractConceptSelectProvider<ConceptValuesConceptSelectDefinition> {

	public ConceptValuesConceptSelectProvider() {
		super(ConceptValuesConceptSelectDefinition.class);
	}

	@Override
	public String type() {
		return "CONCEPT_VALUES";
	}

	@Override
	public DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, ConceptValuesConceptSelectDefinition payload) {
		return select(context, payload, list("STRING"));
	}
}
