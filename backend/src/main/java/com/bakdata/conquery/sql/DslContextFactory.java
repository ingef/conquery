package com.bakdata.conquery.sql;

import javax.sql.DataSource;

import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public class DslContextFactory {

	public static DSLContext create(SqlConnectorConfig config) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(config.getJdbcConnectionUrl());
		hikariConfig.setUsername(config.getDatabaseUsername());
		hikariConfig.setPassword(config.getDatabasePassword());

		DataSource dataSource = new HikariDataSource(hikariConfig);

		return DSL.using(
				dataSource,
				config.getDialect().getJooqDialect(),
				new Settings().withRenderFormatted(config.isWithPrettyPrinting())
		);
	}

}
