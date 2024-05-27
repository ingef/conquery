package com.bakdata.conquery.integration.sql.dialect;

import java.util.Map;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Dataset;

public class TestSqlConnectorConfig extends SqlConnectorConfig {

	private final DatabaseConfig databaseConfig;

	public TestSqlConnectorConfig(DatabaseConfig databaseConfig) {
		super(true, true, Map.of());
		this.databaseConfig = databaseConfig;
	}

	@Override
	public DatabaseConfig getDatabaseConfig(Dataset dataset) {
		return databaseConfig;
	}

}
