package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataRandomSelect", description = "Selects one value of a column.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "RANDOM")
public final class RandomSelectDefinition extends MappableSelectDefinition {
}
