package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataMultiSelectFilter", description = "Selection filter whose values may be embedded in the concept metadata.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "SELECT")
public final class MultiSelectFilterDefinition extends SelectFilterDefinition {
}
