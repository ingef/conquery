package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.DatabaseConnectionConfig;

public interface TestContextProvider {

	DatabaseConnectionConfig getDatabaseConfig();

	TestSqlConnectorConfig getSqlConnectorConfig();

}
