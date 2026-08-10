package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataQuartersInYearSelect", description = "Counts quarters with matching events per year.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "QUARTERS_IN_YEAR")
public final class QuartersInYearSelectDefinition extends SingleColumnSelectDefinition {
}
