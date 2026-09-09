package com.bakdata.conquery.quarkus.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EntityPreviewResponse(
		List<LabeledSource> all,
		@JsonProperty("default")
		List<LabeledSource> defaultSources,
		List<String> searchFilters,
		String searchConcept
) {
	public record LabeledSource(
			String name,
			String label
	) {
	}
}
