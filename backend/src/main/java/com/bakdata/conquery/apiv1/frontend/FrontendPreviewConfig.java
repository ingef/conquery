package com.bakdata.conquery.apiv1.frontend;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.Concept;
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

	@NsIdRef
	private final Concept<?> searchConcept;
}
