package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataQuartersInYearFilter", description = "Range filter over the quarter of a date.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "QUARTERS_IN_YEAR")
public final class QuartersInYearFilterDefinition extends SingleColumnFilterDefinition {
}
