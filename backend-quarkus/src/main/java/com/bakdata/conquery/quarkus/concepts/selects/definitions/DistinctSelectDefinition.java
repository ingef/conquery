package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataDistinctSelect", description = "Selects all distinct values of a column.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "DISTINCT")
public final class DistinctSelectDefinition extends MappableSelectDefinition {
}
