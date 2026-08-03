package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.ExistsConceptSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExistsConceptSelectProvider extends AbstractConceptSelectProvider<ExistsConceptSelectDefinition> {

	public ExistsConceptSelectProvider() {
		super(ExistsConceptSelectDefinition.class);
	}

	@Override
	public String type() {
		return "EXISTS";
	}

	@Override
	public DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, ExistsConceptSelectDefinition payload) {
		return select(context, payload, primitive("BOOLEAN"));
	}
}
