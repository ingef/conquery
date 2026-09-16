package com.bakdata.conquery.quarkus.plugin.api.filters;

import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true,
		defaultImpl = UnknownFilterDefinition.class
)
@Schema(name = "MetadataFilterDefinition", description = "Filter definition used in connector metadata. The concrete model is selected by `type`.", discriminatorProperty = "type")
@PolymorphicModelBase(
		schemaName = "MetadataFilterDefinition",
		description = "Filter definition used in connector metadata. The concrete model is selected by `type`."
)
public interface FilterDefinition {

	String getType();
}
