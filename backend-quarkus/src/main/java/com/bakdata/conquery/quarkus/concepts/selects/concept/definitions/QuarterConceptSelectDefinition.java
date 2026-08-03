package com.bakdata.conquery.quarkus.concepts.selects.concept.definitions;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(name = "MetadataQuarterConceptSelect", description = "Returns the quarter containing a sampled event date.")
public final class QuarterConceptSelectDefinition extends AbstractConceptSelectDefinition {

	@NotNull
	private TemporalSampler sample;

	public enum TemporalSampler {
		EARLIEST,
		LATEST,
		RANDOM
	}
}
