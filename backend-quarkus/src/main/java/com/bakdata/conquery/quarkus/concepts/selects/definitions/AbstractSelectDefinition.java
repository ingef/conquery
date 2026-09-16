package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import com.bakdata.conquery.quarkus.concepts.selects.SelectDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractSelectDefinition implements SelectDefinition {

	@Schema(description = "Select implementation discriminator.")
	private String type;

	@Schema(description = "Stable local name used to form the select id. Falls back to the label when omitted.", pattern = "^\\w+$")
	private String name;

	@Schema(description = "Label displayed to users.")
	private String label;

	@Schema(description = "Additional explanation displayed for the select.")
	private String description;

	@Schema(description = "Whether the frontend preselects this select.")
	private boolean isDefault;
}
