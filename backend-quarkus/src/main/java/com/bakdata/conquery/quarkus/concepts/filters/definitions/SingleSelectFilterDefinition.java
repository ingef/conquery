package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataSingleSelectFilter", description = "Selection filter accepting one value.")
public final class SingleSelectFilterDefinition extends SelectFilterDefinition {
}
