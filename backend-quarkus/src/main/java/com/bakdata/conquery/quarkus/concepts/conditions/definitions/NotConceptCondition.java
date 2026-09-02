package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataNotConceptCondition", description = "Matches when its nested condition does not match.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "NOT")
public final class NotConceptCondition extends AbstractConceptCondition {

	@Valid
	@NotNull
	private ConceptCondition condition;
}
