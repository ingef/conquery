package com.bakdata.conquery.quarkus.plugin.api.filters;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Convenience base for filter definitions referencing one local connector-table column. */
public abstract class SingleColumnFilterDefinition extends AbstractFilterDefinition {

	@NotBlank
	@Schema(description = "Local name of a column in the connector table.", required = true, pattern = "^\\w+$")
	private String column;

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}
}
