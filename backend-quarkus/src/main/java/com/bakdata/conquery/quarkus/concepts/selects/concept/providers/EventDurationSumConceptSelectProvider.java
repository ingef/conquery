package com.bakdata.conquery.quarkus.concepts.selects.concept.providers;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.EventDurationSumConceptSelectDefinition;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventDurationSumConceptSelectProvider extends AbstractConceptSelectProvider<EventDurationSumConceptSelectDefinition> {

	public EventDurationSumConceptSelectProvider() {
		super(EventDurationSumConceptSelectDefinition.class);
	}

	@Override
	public String type() {
		return "EVENT_DURATION_SUM";
	}

	@Override
	public DatasetCatalogRepository.ConceptSelect convert(ConceptSelectConversionContext context, EventDurationSumConceptSelectDefinition payload) {
		return select(context, payload, primitive("INTEGER"));
	}
}
