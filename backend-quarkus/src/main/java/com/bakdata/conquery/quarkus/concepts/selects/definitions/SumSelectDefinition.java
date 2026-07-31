package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataSumSelect", description = "Sums a numeric column, optionally subtracting another column.")
public final class SumSelectDefinition extends SingleColumnSelectDefinition {
	private String subtractColumn;
	private List<String> distinctByColumn;
}
