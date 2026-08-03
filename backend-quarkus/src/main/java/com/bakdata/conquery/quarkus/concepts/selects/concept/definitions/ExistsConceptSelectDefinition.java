package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataExistsConceptSelect", description = "Returns whether the concept has any matching event.")
public final class ExistsConceptSelectDefinition extends AbstractConceptSelectDefinition {
}
