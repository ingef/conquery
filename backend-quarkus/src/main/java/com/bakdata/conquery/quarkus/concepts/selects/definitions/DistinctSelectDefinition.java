package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataDistinctSelect", description = "Selects all distinct values of a column.")
public final class DistinctSelectDefinition extends MappableSelectDefinition {
}
