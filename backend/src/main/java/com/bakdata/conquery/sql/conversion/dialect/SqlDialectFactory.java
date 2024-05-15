package com.bakdata.conquery.sql.conversion.dialect;

import com.bakdata.conquery.models.config.Dialect;

public class SqlDialectFactory {

	public SqlDialect createSqlDialect(Dialect dialect) {
		return switch (dialect) {
			case POSTGRESQL -> new PostgreSqlDialect();
			case HANA -> new HanaSqlDialect();
			case CLICKHOUSE -> new ClickHouseDialect();
		};
	}

}
