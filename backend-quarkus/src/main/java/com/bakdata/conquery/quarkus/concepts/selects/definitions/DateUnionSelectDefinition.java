package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataDateUnionSelect", description = "Returns the union of selected date ranges.")
public final class DateUnionSelectDefinition extends DateRangeSelectDefinition {
}
