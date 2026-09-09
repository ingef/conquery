package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.SingleColumnFilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataNumberFilter", description = "Numeric range filter over one column.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "NUMBER")
public final class NumberFilterDefinition extends SingleColumnFilterDefinition {
}
