package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataBigMultiSelectFilter", description = "Selection filter whose values are fetched by the frontend.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "BIG_MULTI_SELECT")
public final class BigMultiSelectFilterDefinition extends SelectFilterDefinition {
}
