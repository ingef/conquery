package com.bakdata.conquery.quarkus.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.config.SqlRuntimeConfig;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import org.junit.jupiter.api.Test;

class SqlDatasourceRegistryTest {

	@Test
	void resolvesSharedNamedDatasourceOnce() {
		SqlRuntimeConfig.SqlDataSource analytics = () -> SqlRuntimeConfig.SqlDialect.CLICKHOUSE;
		Map<String, SqlRuntimeConfig.SqlDialect> resolved = SqlDatasourceRegistry.resolveDialectMappings(
				List.of(dataset("claims", "analytics"), dataset("diagnoses", "analytics")),
				Map.of("analytics", analytics)
		);

		assertEquals(Map.of("analytics", SqlRuntimeConfig.SqlDialect.CLICKHOUSE), resolved);
	}

	@Test
	void rejectsDatasetWithoutDialectMetadata() {
		IllegalStateException error = assertThrows(
				IllegalStateException.class,
				() -> SqlDatasourceRegistry.resolveDialectMappings(List.of(dataset("claims", "analytics")), Map.of())
		);

		assertTrue(error.getMessage().contains("conquery.sql.datasources.analytics.dialect"));
	}

	private DatasetCatalogRepository.DatasetRecord dataset(String id, String dataSource) {
		return new DatasetCatalogRepository.DatasetRecord(DatasetId.parse(id), id, dataSource);
	}
}
