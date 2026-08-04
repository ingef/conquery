package com.bakdata.conquery.models.config;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.util.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;

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
	@NotNull
	private Dialect dialect;

	/**
	 * Maximum workers to schedule for matching stats. This depends on your HikariCP configuration as well.
	 */
	@Min(1)
	@Builder.Default
	private int matchingStatsWorkers = 5;

	/**
	 * Retries for matching stats. This is a bit of a workaround because some DBMS seem to struggle to properly communicate workload to HikariCP.
	 */
	@Min(1)
	@Builder.Default
	private int matchingStatsRetries = 3;

	/**
	 * Name of the column which is shared among the table and all aggregations are grouped by.
	 */
	@Builder.Default
	@NotNull
	private String primaryColumn = DEFAULT_PRIMARY_COLUMN;

	private String propertiesFile;

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


	public HikariDataSource createDataSource(HealthCheckRegistry healthCheckRegistry) {

		// If propertiesFile is provided start with that and overwrite with internal settings. Allows us to use both.
		HikariConfig hikariConfig = propertiesFile != null ? new HikariConfig(propertiesFile) : new HikariConfig();

		if (getJdbcConnectionUrl() != null) {
			hikariConfig.setJdbcUrl(getJdbcConnectionUrl());
		}

		if (getDatabaseUsername() != null) {
			hikariConfig.setUsername(getDatabaseUsername());
		}
		if (getDatabasePassword() != null) {
			hikariConfig.setPassword(getDatabasePassword());
		}

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
