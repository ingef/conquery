package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPrefixListConceptCondition", description = "Matches values beginning with one of the configured prefixes.")
public final class PrefixListConceptCondition extends AbstractConceptCondition {

	@NotEmpty
	private List<String> prefixes;
}
