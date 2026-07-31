package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataFirstSelect", description = "Selects the first value of a column.")
public final class FirstSelectDefinition extends MappableSelectDefinition {
}
