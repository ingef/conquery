package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataRandomSelect", description = "Selects one value of a column.")
public final class RandomSelectDefinition extends MappableSelectDefinition {
}
