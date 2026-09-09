package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataEventDateUnionConceptSelect", description = "Returns the union of matching event dates.")
@PolymorphicModelSubtype(base = ConceptSelectDefinition.class, id = "EVENT_DATE_UNION")
public final class EventDateUnionConceptSelectDefinition extends AbstractConceptSelectDefinition {
}
