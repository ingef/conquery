package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.DatabaseConnection;
import org.jooq.DSLContext;

public interface TestContextProvider {

	DatabaseConnection getDatabaseConfig();

	TestSqlConnectorConfig getSqlConnectorConfig();

	DSLContext getDslContext();

}
