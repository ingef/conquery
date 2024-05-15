package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialectFactory;

public class TestSqlDialectFactory extends SqlDialectFactory {

	@Override
	public SqlDialect createSqlDialect(Dialect dialect) {
		return switch (dialect) {
			case POSTGRESQL -> new PostgreSqlIntegrationTests.TestPostgreSqlDialect();
			case HANA -> new HanaSqlIntegrationTests.TestHanaDialect();
			case CLICKHOUSE -> new ClickHouseIntegrationTests.TestClickHouseDialect();
		};
	}
}
