package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataAndConceptCondition", description = "Matches when all nested conditions match.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "AND")
public final class AndConceptCondition extends AbstractConceptCondition {

	@Valid
	@NotEmpty
	private List<ConceptCondition> conditions;
}
