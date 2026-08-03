package com.bakdata.conquery.quarkus.concepts.conditions;

import com.bakdata.conquery.quarkus.models.PolymorphicModelBase;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true,
		defaultImpl = UnknownConceptCondition.class
)
@Schema(name = "MetadataConceptCondition", description = "Condition assigned to a concept-tree node. The concrete model is selected by `type`.", discriminatorProperty = "type")
@PolymorphicModelBase(
		schemaName = "MetadataConceptCondition",
		description = "Condition assigned to a concept-tree node. The concrete model is selected by `type`."
)
public interface ConceptCondition {

	String getType();
}
