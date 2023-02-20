package com.bakdata.conquery.apiv1.frontend;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
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

	@NsIdRefCollection
	private final List<Filter<?>> searchFilters;
}
