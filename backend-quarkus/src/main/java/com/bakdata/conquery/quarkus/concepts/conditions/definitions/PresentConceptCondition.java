package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataPresentConceptCondition", description = "Matches when the configured column in the same record/event has a value present.")
public final class PresentConceptCondition extends AbstractConceptCondition {

	@NotBlank
	private String column;
}
