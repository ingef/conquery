package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "MetadataDateUnionSelect", description = "Returns the union of selected date ranges.")
@PolymorphicModelSubtype(base = SelectDefinition.class, id = "DATE_UNION")
public final class DateUnionSelectDefinition extends DateRangeSelectDefinition {
}
