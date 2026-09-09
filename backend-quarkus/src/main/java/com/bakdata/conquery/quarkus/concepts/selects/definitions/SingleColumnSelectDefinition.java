package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public abstract class SingleColumnSelectDefinition extends AbstractSelectDefinition {

	@NotBlank
	@Schema(description = "Local name of the selected column.")
	private String column;

	@Schema(description = "Whether values form a bounded categorical set.")
	private boolean categorical;
}
