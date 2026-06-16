package com.bakdata.conquery.quarkus.api.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "conquery")
public interface DatasetsRuntimeConfig {
	/**
	 * Static dataset definitions used when no generated metadata folder is mounted.
	 */
	Optional<List<DatasetEntry>> datasets();

	interface DatasetEntry {
		/**
		 * Stable dataset id used in API paths and dataset-scoped storage.
		 */
		String id();

		/**
		 * Human-readable dataset label shown in frontend dataset selection.
		 */
		String label();
	}
}
