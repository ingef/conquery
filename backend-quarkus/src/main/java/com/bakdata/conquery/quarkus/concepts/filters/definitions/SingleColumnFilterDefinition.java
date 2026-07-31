package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public abstract class SingleColumnFilterDefinition extends AbstractFilterDefinition {

	@NotBlank
	@Schema(description = "Local name of a column in the connector table.", required = true, pattern = "^\\w+$")
	private String column;
}
