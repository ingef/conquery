package com.bakdata.conquery.quarkus.api.config;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "conquery")
public interface ConceptsRuntimeConfig {
	List<ConceptEntry> concepts();

	interface ConceptEntry {
		String id();

		String label();

		String dataset();
	}
}
