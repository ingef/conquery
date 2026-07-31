package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataPrefixTextFilter", description = "Text-prefix filter over one string column.")
public final class PrefixTextFilterDefinition extends SingleColumnFilterDefinition {
}
