package com.bakdata.conquery.quarkus.config;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "conquery.sql")
public interface SqlRuntimeConfig {

	/**
	 * Enables validation and use of SQL datasources for dataset queries.
	 */
	@WithDefault("false")
	boolean enabled();

	/**
	 * SQL dialect metadata keyed by the corresponding Quarkus named datasource.
	 */
	Map<String, SqlDataSource> datasources();

	interface SqlDataSource {
		SqlDialect dialect();
	}

	enum SqlDialect {
		CLICKHOUSE,
		HANA
	}
}
