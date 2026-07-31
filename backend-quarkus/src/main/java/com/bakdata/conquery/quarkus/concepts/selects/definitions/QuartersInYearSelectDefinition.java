package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataQuartersInYearSelect", description = "Counts quarters with matching events per year.")
public final class QuartersInYearSelectDefinition extends SingleColumnSelectDefinition {
}
