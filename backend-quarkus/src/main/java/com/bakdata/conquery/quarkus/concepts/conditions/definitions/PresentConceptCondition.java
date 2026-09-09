package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPresentConceptCondition", description = "Matches when the configured column in the same record/event has a value present.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "PRESENT")
public final class PresentConceptCondition extends AbstractConceptCondition {

	@NotBlank
	private String column;
}
