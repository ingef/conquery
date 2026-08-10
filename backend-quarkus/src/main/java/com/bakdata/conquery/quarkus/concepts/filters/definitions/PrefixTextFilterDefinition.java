package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataPrefixTextFilter", description = "Text-prefix filter over one string column.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "PREFIX_TEXT")
public final class PrefixTextFilterDefinition extends SingleColumnFilterDefinition {
}
