package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
public abstract class MappableSelectDefinition extends SingleColumnSelectDefinition {

	@Schema(description = "Optional mapping applied to selected values.")
	private String mapping;

	@Schema(description = "Optional substring range.")
	private IntegerRangeDefinition substring;

	public record IntegerRangeDefinition(Integer min, Integer max) {
	}
}
