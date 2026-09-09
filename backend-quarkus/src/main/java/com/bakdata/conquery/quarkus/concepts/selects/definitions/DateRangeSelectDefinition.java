package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public abstract class DateRangeSelectDefinition extends AbstractSelectDefinition {

	@Schema(description = "Local DATE_RANGE column; alternatively use startColumn and endColumn.")
	private String column;

	@Schema(description = "Local range start column.")
	private String startColumn;

	@Schema(description = "Local range end column.")
	private String endColumn;
}
