package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataNumberFilter", description = "Numeric range filter over one column.")
public final class NumberFilterDefinition extends SingleColumnFilterDefinition {
}
