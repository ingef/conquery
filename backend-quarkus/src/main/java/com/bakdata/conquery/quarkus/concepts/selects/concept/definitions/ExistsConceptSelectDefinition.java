package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataExistsConceptSelect", description = "Returns whether the concept has any matching event.")
@PolymorphicModelSubtype(base = ConceptSelectDefinition.class, id = "EXISTS")
public final class ExistsConceptSelectDefinition extends AbstractConceptSelectDefinition {
}
