package com.bakdata.conquery.quarkus.concepts.conditions.definitions;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataEqualConceptCondition", description = "Matches when a value is contained exactly in this list.")
public final class EqualConceptCondition extends AbstractConceptCondition {

	@NotEmpty
	private List<String> values;
}
