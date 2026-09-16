package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataCountQuartersSelect", description = "Counts distinct quarters covered by dates or date ranges.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "COUNT_QUARTERS")
public final class CountQuartersSelectDefinition extends DateRangeSelectDefinition {
}
