package com.bakdata.conquery.integration.sql.dialect;

import java.util.Map;

import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonDeserialize(as = TestSqlConnectorConfig.class)
public class TestSqlConnectorConfig extends SqlConnectorConfig {

	private static final String TEST_DATASET = "test";

	public TestSqlConnectorConfig(DatabaseConfig databaseConfig) {
		super(true, true, Runtime.getRuntime().availableProcessors(), Map.of(TEST_DATASET, databaseConfig), null);
	}

	@Override
	public DatabaseConfig getDatabaseConfig(Dataset dataset) {
		return getDatabaseConfigs().get(TEST_DATASET);
	}

}
