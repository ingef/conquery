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
@Schema(name = "MetadataOrConceptCondition", description = "Matches when at least one nested condition matches.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "OR")
public final class OrConceptCondition extends AbstractConceptCondition {

	@Valid
	@NotEmpty
	private List<ConceptCondition> conditions;
}
