package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataDurationSumFilter", description = "Range filter over summed date-range durations.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "DURATION_SUM")
public final class DurationSumFilterDefinition extends SingleColumnFilterDefinition {
	@Schema(description = "Local columns used to remove duplicate date ranges before summing durations.")
	private List<String> distinctBy;
}
