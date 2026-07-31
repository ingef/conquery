package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataCountQuartersSelect", description = "Counts distinct quarters covered by dates or date ranges.")
public final class CountQuartersSelectDefinition extends DateRangeSelectDefinition {
}
