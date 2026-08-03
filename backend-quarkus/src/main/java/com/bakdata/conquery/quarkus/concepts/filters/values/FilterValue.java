package com.bakdata.conquery.quarkus.concepts.filters.values;

import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Schema(name = "QueryFilterValue", description = "Value assigned to a concept filter in a query. The concrete value model is selected by `type`.", discriminatorProperty = "type")
@PolymorphicModelBase(
		schemaName = "QueryFilterValue",
		description = "Value assigned to a concept filter in a query. The concrete value model is selected by `type`."
)
public interface FilterValue {

	FilterId filter();
}
