package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataCountQuartersFilter", description = "Range filter over the number of quarters covered by a date range.")
public final class CountQuartersFilterDefinition extends SingleColumnFilterDefinition {
}
