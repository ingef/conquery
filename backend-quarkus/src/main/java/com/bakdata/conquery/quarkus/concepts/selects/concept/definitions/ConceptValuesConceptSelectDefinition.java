package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.concept.ConceptSelectDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataConceptValuesConceptSelect", description = "Returns values of matching concept nodes.")
@PolymorphicModelSubtype(base = ConceptSelectDefinition.class, id = "CONCEPT_VALUES")
public final class ConceptValuesConceptSelectDefinition extends AbstractConceptSelectDefinition {

	@Schema(description = "Whether concept ids are returned instead of labels.")
	private boolean asIds;
}
