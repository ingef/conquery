package com.bakdata.conquery.quarkus.concepts.selects.definitions;

import java.time.temporal.ChronoUnit;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataDateDistanceSelect", description = "Calculates a date distance in the configured unit.")
public final class DateDistanceSelectDefinition extends SingleColumnSelectDefinition {
	private ChronoUnit timeUnit = ChronoUnit.YEARS;
}
