package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataCategoryMaxSumFilter", description = "Numeric range filter over category-wise maximum sums.")
// TODO May rename this to DistincMaxSumFilter
public final class CategoryMaxSumFilterDefinition extends AbstractFilterDefinition {
	@Schema(description = "Category column.")
	@JsonAlias("category")
	private String categoryColumn;
	@Schema(description = "Value column.")
	@JsonAlias("value")
	private String valueColumn;
}
