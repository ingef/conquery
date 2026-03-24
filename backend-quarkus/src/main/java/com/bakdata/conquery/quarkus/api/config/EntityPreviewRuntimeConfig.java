package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "conquery.entity-preview")
public interface EntityPreviewRuntimeConfig {

	@WithName("all")
	List<Source> allSources();

	@WithName("default")
	List<Source> defaultSources();

	Optional<String> searchConcept();

	Optional<String> searchFilters();

	interface Source {
		String name();

		String label();
	}
}
