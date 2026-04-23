package com.bakdata.conquery.quarkus.api.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.storage")
public interface StorageRuntimeConfig {
	@WithDefault("IN_MEMORY")
	String backend();
}
