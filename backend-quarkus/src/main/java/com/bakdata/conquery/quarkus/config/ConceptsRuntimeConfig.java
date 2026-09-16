package com.bakdata.conquery.quarkus.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "conquery")
public interface ConceptsRuntimeConfig {
	/**
	 * Static root concept definitions used when no generated metadata folder is mounted.
	 */
	Optional<List<ConceptEntry>> concepts();

	interface ConceptEntry {
		/**
		 * Concept id as exposed to the frontend API.
		 */
		String id();

		/**
		 * Human-readable concept label shown in the concept tree.
		 */
		String label();

		/**
		 * Human-readable concept description shown in the concept tree.
		 */
		String description();


		/**
		 * Dataset id this concept belongs to.
		 */
		String dataset();
	}
}
