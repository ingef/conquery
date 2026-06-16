package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.metadata")
public interface DatasetMetadataRuntimeConfig {

	/**
	 * Enables startup ingestion of generated dataset metadata folders.
	 */
	@WithDefault("false")
	boolean enabled();

	/**
	 * Base directory that contains generated dataset metadata folders.
	 */
	Optional<String> rootPath();

	/**
	 * Folder names or absolute paths to ingest as static dataset metadata.
	 */
	Optional<List<String>> folders();
}
