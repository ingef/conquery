package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.DSLContextWrapper;

public interface TestContextProvider {

	DatabaseConfig getDatabaseConfig();

	SqlConnectorConfig getSqlConnectorConfig();

	DSLContextWrapper getDslContextWrapper();

}
