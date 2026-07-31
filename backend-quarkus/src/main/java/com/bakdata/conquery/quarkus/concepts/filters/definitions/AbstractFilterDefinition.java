package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractFilterDefinition implements FilterDefinition {

	@Schema(description = "Filter implementation discriminator.")
	private String type;

	@Schema(description = "Stable local name used to form the filter id. Falls back to the label when omitted.", pattern = "^\\w+$")
	private String name;

	@Schema(description = "Label displayed to users.")
	private String label;

	@JsonAlias("description")
	@Schema(description = "Additional explanation displayed for the filter.")
	private String tooltip;

	@Schema(description = "Unit displayed with filter values.")
	private String unit;

	@Schema(description = "Optional input validation pattern.")
	private String pattern;

	@Schema(description = "Whether values may be supplied using a dropped file.")
	private Boolean allowDropFile;

	@Schema(description = "Default frontend value.")
	private Object defaultValue;

	@Schema(description = "Optional minimum frontend value.")
	private Integer min;

	@Schema(description = "Optional maximum frontend value.")
	private Integer max;
}
