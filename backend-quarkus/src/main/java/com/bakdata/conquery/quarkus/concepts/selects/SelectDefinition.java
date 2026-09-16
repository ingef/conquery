package com.bakdata.conquery.quarkus.concepts.selects;

import com.bakdata.conquery.quarkus.models.PolymorphicModelBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true,
		defaultImpl = UnknownSelectDefinition.class
)
@Schema(name = "MetadataSelectDefinition", description = "Select definition used in connector metadata. The concrete model is selected by `type`.", discriminatorProperty = "type")
@PolymorphicModelBase(
		schemaName = "MetadataSelectDefinition",
		description = "Select definition used in connector metadata. The concrete model is selected by `type`."
)
public interface SelectDefinition {

	String getType();
}
