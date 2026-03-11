package com.bakdata.conquery.models.config;

import java.io.Closeable;
import java.io.IOException;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.conf.RenderOptionalKeyword;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

/**
 * Connection properties for a SQL database.
 * <p/>
 * Currently supported are HANA and Prostgres databases, see {@link DatabaseConnection#dialect}.
 */
@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class DatabaseConnection implements Closeable, Managed {

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

	@JsonIgnore
	private HikariDataSource dataSource;

	@JsonIgnore
	private HealthCheckRegistry healthCheckRegistry;

	@Override
	public void start() throws Exception {
		initializeDataSource();
	}

	public void initializeDataSource() {
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

		dataSource = new HikariDataSource(hikariConfig);

		try {
			log.debug("TESTING connection {}", getJdbcConnectionUrl());
			DSLContext dslContext = DSL.using(this.dataSource, getDialect().getJooqDialect());
			dslContext.execute(getDialect().getTestConnection());
			log.debug("SUCCESS connecting to {}", getJdbcConnectionUrl());
		}catch (Exception exception) {
			log.error("FAILED to connect to {}", getJdbcConnectionUrl(), exception);
		}
	}

	public DSLContext connect(SqlConnectorConfig connectorConfig) {
		Preconditions.checkNotNull(this.dataSource, "dataSource has not been initialized yet.");

		Settings settings = new Settings()
				.withRenderFormatted(connectorConfig.isWithPrettyPrinting())
				// enforces all identifiers to be quoted if not explicitly unquoted via DSL.unquotedName()
				// to prevent any lowercase/uppercase SQL dialect specific identifier naming issues
				.withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED)
				// always render "as" keyword for field aliases
				.withRenderOptionalAsKeywordForFieldAliases(RenderOptionalKeyword.ON);

		return DSL.using(
				this.dataSource,
				getDialect().getJooqDialect(),
				settings
		);
	}

	@Override
	public void close() throws IOException {
		dataSource.close();
	}
}
