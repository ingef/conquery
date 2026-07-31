package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataBigMultiSelectFilter", description = "Selection filter whose values are fetched by the frontend.")
public final class BigMultiSelectFilterDefinition extends SelectFilterDefinition {
}
