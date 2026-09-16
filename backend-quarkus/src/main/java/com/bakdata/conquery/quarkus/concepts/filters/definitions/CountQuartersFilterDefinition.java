package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.SingleColumnFilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataCountQuartersFilter", description = "Range filter over the number of quarters covered by a date range.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "COUNT_QUARTERS")
public final class CountQuartersFilterDefinition extends SingleColumnFilterDefinition {
}
