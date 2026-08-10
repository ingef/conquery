package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPrefixListConceptCondition", description = "Matches values beginning with one of the configured prefixes.")
@PolymorphicModelSubtype(base = ConceptCondition.class, id = "PREFIX_LIST")
public final class PrefixListConceptCondition extends AbstractConceptCondition {

	@NotEmpty
	private List<String> prefixes;
}
