package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataEventDurationSumConceptSelect", description = "Returns the total duration of matching events.")
@PolymorphicModelSubtype(base = ConceptSelectDefinition.class, id = "EVENT_DURATION_SUM")
public final class EventDurationSumConceptSelectDefinition extends AbstractConceptSelectDefinition {
}
