package com.bakdata.conquery.quarkus.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.storage")
public interface StorageRuntimeConfig {
	/**
	 * Storage backend used for manager metadata and dataset catalog repositories.
	 */
	@WithDefault("IN_MEMORY")
	String backend();

	/**
	 * Xodus-specific storage settings used when the XODUS backend is enabled.
	 */
	Xodus xodus();

	interface Xodus {
		/**
		 * Base directory for Xodus-backed manager metadata and dataset-scoped environments.
		 */
		@WithDefault("storage/quarkus-meta")
		String path();
	}
}
