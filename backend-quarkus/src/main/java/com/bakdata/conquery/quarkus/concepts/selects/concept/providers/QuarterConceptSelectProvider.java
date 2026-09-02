package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.QuarterConceptSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuarterConceptSelectProvider extends AbstractConceptSelectProvider<QuarterConceptSelectDefinition> {

	public QuarterConceptSelectProvider() {
		super(QuarterConceptSelectDefinition.class);
	}


	@Override
	public DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, QuarterConceptSelectDefinition payload) {
		return select(context, payload, primitive("STRING"));
	}
}
