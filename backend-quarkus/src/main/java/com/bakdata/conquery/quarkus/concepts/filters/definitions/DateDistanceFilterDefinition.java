package com.bakdata.conquery.quarkus.concepts.filters.definitions;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.models.PolymorphicModelSubtype;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataDateDistanceFilter", description = "Range filter over the distance between a date and the reference date.")
@PolymorphicModelSubtype(base = FilterDefinition.class, id = "DATE_DISTANCE")
public final class DateDistanceFilterDefinition extends SingleColumnFilterDefinition {
	@Schema(description = "Unit used to measure the date distance, for example YEARS or DAYS.", defaultValue = "YEARS")
	private String timeUnit;
}
