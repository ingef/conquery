package com.bakdata.conquery.quarkus.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.storage")
public interface StorageRuntimeConfig {
	/**
	 * Storage backend used for persistent manager metadata. Dataset definitions are always loaded from metadata folders at startup.
	 */
	@WithDefault("IN_MEMORY")
	String backend();

	/**
	 * Xodus-specific storage settings used when the XODUS backend is enabled.
	 */
	Xodus xodus();

	interface Xodus {
		/**
		 * Base directory for Xodus-backed manager metadata.
		 */
		@WithDefault("storage/quarkus-meta")
		String path();
	}
}
