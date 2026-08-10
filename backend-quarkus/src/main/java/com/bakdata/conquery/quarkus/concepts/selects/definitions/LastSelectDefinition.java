package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataLastSelect", description = "Selects the last value of a column.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "LAST")
public final class LastSelectDefinition extends MappableSelectDefinition {
}
