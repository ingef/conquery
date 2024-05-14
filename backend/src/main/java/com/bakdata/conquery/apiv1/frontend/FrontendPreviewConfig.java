package com.bakdata.conquery.apiv1.frontend;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FrontendPreviewConfig {
	@Data
	public static class Labelled {
		private final String name;
		private final String label;
	}

	private final Collection<Labelled> all;
	@JsonProperty("default")
	private final Collection<Labelled> defaultConnectors;

	private final List<FilterId> searchFilters;

	/**
	 * Search concept needs to be parent of searchFilters, so frontend can resolve the filters.
	 */
	private final ConceptId searchConcept;
}
