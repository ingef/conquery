package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataLastSelect", description = "Selects the last value of a column.")
public final class LastSelectDefinition extends MappableSelectDefinition {
}
