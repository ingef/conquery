package com.bakdata.conquery.quarkus.concepts.selects.concept;

import com.bakdata.conquery.quarkus.models.PolymorphicModelBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true,
		defaultImpl = UnknownConceptSelectDefinition.class
)
@Schema(name = "MetadataConceptSelectDefinition", description = "Select definition used at concept level. The concrete model is selected by `type`.", discriminatorProperty = "type")
@PolymorphicModelBase(
		schemaName = "MetadataConceptSelectDefinition",
		description = "Select definition used at concept level. The concrete model is selected by `type`."
)
public interface ConceptSelectDefinition {

	String getType();
}
