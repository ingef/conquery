package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.EventDateUnionConceptSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventDateUnionConceptSelectProvider extends AbstractConceptSelectProvider<EventDateUnionConceptSelectDefinition> {

	public EventDateUnionConceptSelectProvider() {
		super(EventDateUnionConceptSelectDefinition.class);
	}

	@Override
	public String type() {
		return "EVENT_DATE_UNION";
	}

	@Override
	public DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, EventDateUnionConceptSelectDefinition payload) {
		return select(context, payload, list("DATE_RANGE"));
	}
}
