package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractConceptCondition implements ConceptCondition {

	@Schema(description = "Concept-condition implementation discriminator.")
	private String type;
}
