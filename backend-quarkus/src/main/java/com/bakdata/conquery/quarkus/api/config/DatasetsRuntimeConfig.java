package com.bakdata.conquery.quarkus.api.config;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "conquery")
public interface DatasetsRuntimeConfig {
	List<DatasetEntry> datasets();

	interface DatasetEntry {
		String id();

		String label();
	}
}
