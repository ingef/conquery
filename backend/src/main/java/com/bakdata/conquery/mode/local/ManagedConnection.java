package com.bakdata.conquery.mode.local;

import java.sql.SQLException;

import javax.annotation.CheckForNull;

import com.bakdata.conquery.models.config.DatabaseConnectionConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.lifecycle.Managed;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.conf.RenderOptionalKeyword;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

@Data
@Slf4j
public class ManagedConnection implements Managed {
	private final SqlConnectorConfig config;
	private final DatabaseConnectionConfig connection;
	@CheckForNull
	private final HealthCheckRegistry healthCheckRegistry;

	private HikariDataSource dataSource;

	@Override
	public void start() throws Exception {
		dataSource = connection.createDataSource(healthCheckRegistry);

		try {
			log.debug("TEST connecting to {}", connection.getJdbcConnectionUrl());
			if (dataSource.getConnection().isValid(100)) {
				log.info("SUCCESS connecting to {}", connection.getJdbcConnectionUrl());
			}
			else {
				log.error("FAILED connecting to {}. Connection did not become valid.", connection.getJdbcConnectionUrl());
			}
		}
		catch (SQLException exception) {
			log.error("FAILED connecting to {}", connection.getJdbcConnectionUrl(), exception);
		}
	}

	public DSLContext connect() {
		Preconditions.checkNotNull(this.dataSource, "dataSource has not been initialized yet.");

		Settings settings = new Settings()
				.withRenderFormatted(config.isWithPrettyPrinting())
				// enforces all identifiers to be quoted if not explicitly unquoted via DSL.unquotedName()
				// to prevent any lowercase/uppercase SQL dialect specific identifier naming issues
				.withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED)
				// always render "as" keyword for field aliases
				.withRenderOptionalAsKeywordForFieldAliases(RenderOptionalKeyword.ON);

		return DSL.using(
				this.dataSource,
				connection.getDialect().getJooqDialect(),
				settings
		);
	}

}
