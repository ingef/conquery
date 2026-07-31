package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataQuartersInYearFilter", description = "Range filter over the quarter of a date.")
public final class QuartersInYearFilterDefinition extends SingleColumnFilterDefinition {
}
