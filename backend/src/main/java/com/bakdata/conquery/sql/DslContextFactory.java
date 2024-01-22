package com.bakdata.conquery.sql;

import javax.sql.DataSource;

import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public class DslContextFactory {

	public static DSLContext create(SqlConnectorConfig config) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(config.getJdbcConnectionUrl());
		hikariConfig.setUsername(config.getDatabaseUsername());
		hikariConfig.setPassword(config.getDatabasePassword());

		DataSource dataSource = new HikariDataSource(hikariConfig);

		Settings settings = new Settings()
				.withRenderFormatted(config.isWithPrettyPrinting())
				// enforces all identifiers to be quoted if not explicitly unquoted via DSL.unquotedName()
				// to prevent any lowercase/uppercase SQL dialect specific identifier naming issues
				.withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED);

		return DSL.using(
				dataSource,
				config.getDialect().getJooqDialect(),
				settings
		);
	}

}
