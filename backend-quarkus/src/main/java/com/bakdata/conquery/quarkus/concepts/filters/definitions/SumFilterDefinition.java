package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.filters.SingleColumnFilterDefinition;
import com.bakdata.conquery.quarkus.plugin.api.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataSumFilter", description = "Numeric range filter over a sum or difference of sums.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "SUM")
public final class SumFilterDefinition extends SingleColumnFilterDefinition {
	@Schema(description = "Optional local column subtracted from the primary sum column.")
	private String subtractColumn;
	@Schema(description = "Local columns used to remove duplicate values before summing.")
	private List<String> distinctByColumn;
}
