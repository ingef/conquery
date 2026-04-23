package com.bakdata.conquery.models.config;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

/**
 * Connection properties for a SQL database.
 * <p/>
 * Currently supported are HANA and Prostgres databases, see {@link DatabaseConnectionConfig#dialect}.
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class DatabaseConnectionConfig {

	private static final String DEFAULT_PRIMARY_COLUMN = "pid";

	/**
	 * SQL vendor specific dialect used to transform queries to SQL
	 */
	private Dialect dialect;

	/**
	 * Username used to connect to the database.
	 */
	private String databaseUsername;


	/**
	 * Password used to connect to the database.
	 */
	@ToString.Exclude
	private String databasePassword;

	/**
	 * Connections url in JDBC notation.
	 */
	private String jdbcConnectionUrl;

	private Duration connectivityTimeout;

	/**
	 * Name of the column which is shared among the table and all aggregations are grouped by.
	 */
	@Builder.Default
	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;


	public HikariDataSource createDataSource(HealthCheckRegistry healthCheckRegistry) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(getJdbcConnectionUrl());
		hikariConfig.setUsername(getDatabaseUsername());
		hikariConfig.setPassword(getDatabasePassword());

		if (healthCheckRegistry != null) {
			hikariConfig.setHealthCheckRegistry(healthCheckRegistry);
			if (getConnectivityTimeout() != null) {
				long connectivityTimeoutMs = getConnectivityTimeout().toMilliseconds();
				hikariConfig.addHealthCheckProperty("connectivityCheckTimeoutMs", Long.toString(connectivityTimeoutMs));
			}
		}

		return new HikariDataSource(hikariConfig);
	}
}
