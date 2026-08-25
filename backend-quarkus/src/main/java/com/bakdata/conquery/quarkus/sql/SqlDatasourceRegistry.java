package com.bakdata.conquery.quarkus.sql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.config.SqlRuntimeConfig;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
public class SqlDatasourceRegistry {

	@Inject
	DatasetCatalogRepository datasetRepository;

	@Inject
	SqlRuntimeConfig sqlConfig;

	@Inject
	@Any
	Instance<AgroalDataSource> agroalDataSources;

	private Map<String, SqlDatasource> dataSourcesByName = Map.of();

	@PostConstruct
	void init() {
		if (!sqlConfig.enabled()) {
			return;
		}

		Map<String, SqlDatasource> resolved = new LinkedHashMap<>();
		resolveDialectMappings(datasetRepository.listDatasets(), sqlConfig.datasources()).forEach((name, dialect) ->
				resolved.put(name, new SqlDatasource(name, dialect, resolveAgroalDatasource(name)))
		);
		dataSourcesByName = Map.copyOf(resolved);
	}

	static Map<String, SqlRuntimeConfig.SqlDialect> resolveDialectMappings(
			List<DatasetCatalogRepository.DatasetRecord> datasets,
			Map<String, SqlRuntimeConfig.SqlDataSource> configuredDataSources
	) {
		Map<String, SqlRuntimeConfig.SqlDialect> resolved = new LinkedHashMap<>();
		for (DatasetCatalogRepository.DatasetRecord dataset : datasets) {
			String name = dataset.dataSource();
			SqlRuntimeConfig.SqlDataSource config = Optional.ofNullable(configuredDataSources.get(name))
					.orElseThrow(() -> new IllegalStateException(
							"Dataset '" + dataset.id() + "' references SQL datasource '" + name
							+ "', but conquery.sql.datasources." + name + ".dialect is not configured."
					));
			resolved.putIfAbsent(name, config.dialect());
		}
		return Map.copyOf(resolved);
	}

	public Optional<SqlDatasource> find(String name) {
		return Optional.ofNullable(dataSourcesByName.get(name));
	}

	public SqlDatasource requireFor(DatasetCatalogRepository.DatasetRecord dataset) {
		return find(dataset.dataSource()).orElseThrow(() -> new IllegalStateException(
				"No active SQL datasource is registered for dataset '" + dataset.id() + "': " + dataset.dataSource()
		));
	}

	private AgroalDataSource resolveAgroalDatasource(String name) {
		Instance<AgroalDataSource> selected = agroalDataSources.select(new DataSource.DataSourceLiteral(name));
		if (!selected.isResolvable()) {
			throw new IllegalStateException(
					"SQL datasource '" + name + "' is not a configured Quarkus named datasource. "
					+ "Configure quarkus.datasource.\"" + name + "\".db-kind and its JDBC settings."
			);
		}
		return selected.get();
	}

	public record SqlDatasource(
			String name,
			SqlRuntimeConfig.SqlDialect dialect,
			AgroalDataSource dataSource
	) {
	}
}
