package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.sql.DSLContextWrapper;

public interface TestContextProvider {

	DatabaseConfig getDatabaseConfig();

	TestSqlConnectorConfig getSqlConnectorConfig();

	DSLContextWrapper getDslContextWrapper();

}
