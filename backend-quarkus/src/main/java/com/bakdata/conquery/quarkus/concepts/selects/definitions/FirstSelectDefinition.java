package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataFirstSelect", description = "Selects the first value of a column.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "FIRST")
public final class FirstSelectDefinition extends MappableSelectDefinition {
}
