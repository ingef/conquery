package com.bakdata.conquery.quarkus.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "conquery.entity-preview")
public interface EntityPreviewRuntimeConfig {

	/**
	 * All entity preview sources available for frontend previews.
	 */
	@WithName("all")
	List<Source> allSources();

	/**
	 * Entity preview sources selected by default.
	 */
	@WithName("default")
	List<Source> defaultSources();

	/**
	 * Optional concept id used as the entry point for entity preview search.
	 */
	Optional<String> searchConcept();

	/**
	 * Optional comma-separated list of filter ids available for entity preview search.
	 */
	Optional<String> searchFilters();

	interface Source {
		/**
		 * Stable source id used by the frontend.
		 */
		String name();

		/**
		 * Human-readable source label.
		 */
		String label();
	}
}
