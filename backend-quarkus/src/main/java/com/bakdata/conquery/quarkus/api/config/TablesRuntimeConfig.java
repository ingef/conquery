package com.bakdata.conquery.quarkus.api.config;

import java.util.List;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery")
public interface TablesRuntimeConfig {
	List<TableEntry> tables();

	interface TableEntry {
		String id();

		String label();

		String dataset();

		@WithDefault("")
		String primaryColumn();

		List<ColumnEntry> columns();
	}

	interface ColumnEntry {
		String id();

		String label();

		String type();

		@WithDefault("__unset__")
		String secondaryId();
	}
}
