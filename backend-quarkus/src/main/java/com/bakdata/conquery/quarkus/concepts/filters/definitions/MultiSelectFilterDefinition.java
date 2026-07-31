package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataMultiSelectFilter", description = "Selection filter whose values may be embedded in the concept metadata.")
public final class MultiSelectFilterDefinition extends SelectFilterDefinition {
}
