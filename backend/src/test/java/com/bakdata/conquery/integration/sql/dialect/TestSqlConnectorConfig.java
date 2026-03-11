package com.bakdata.conquery.integration.sql.dialect;

import java.util.Map;

import com.bakdata.conquery.models.config.DatabaseConnection;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@JsonDeserialize(as = TestSqlConnectorConfig.class)
public class TestSqlConnectorConfig extends SqlConnectorConfig {

	private static final String TEST_DATASET = "test";

	public TestSqlConnectorConfig(DatabaseConnection databaseConfig) {
		super(true, true, Map.of(new DatasetId(TEST_DATASET), databaseConfig));
	}

	@Override
	public DatabaseConnection getDatabaseConfig(Dataset dataset) {
		return getDatabaseConfigs().get(new DatasetId(TEST_DATASET));
	}

}
