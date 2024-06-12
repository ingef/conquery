package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conquery.SqlExecutionManager;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialectFactory;
import com.bakdata.conquery.sql.execution.ResultSetProcessorFactory;
import com.bakdata.conquery.sql.execution.SqlExecutionResult;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.codahale.metrics.MetricRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Slf4j
public class LocalNamespaceHandler implements NamespaceHandler<LocalNamespace> {

	private final ConqueryConfig config;
	private final InternalObjectMapperCreator mapperCreator;
	private final SqlDialectFactory dialectFactory;

	@Override
	public LocalNamespace createNamespace(NamespaceStorage namespaceStorage, MetaStorage metaStorage, IndexService indexService, MetricRegistry metricRegistry) {

		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(namespaceStorage, config, mapperCreator, indexService, metricRegistry);

		SqlConnectorConfig sqlConnectorConfig = config.getSqlConnectorConfig();
		DatabaseConfig databaseConfig = sqlConnectorConfig.getDatabaseConfig(namespaceStorage.getDataset());

		DSLContextWrapper dslContextWrapper = DslContextFactory.create(databaseConfig, sqlConnectorConfig);
		DSLContext dslContext = dslContextWrapper.getDslContext();
		SqlDialect sqlDialect = dialectFactory.createSqlDialect(databaseConfig.getDialect());

		SqlConverter sqlConverter = new SqlConverter(sqlDialect, dslContext, databaseConfig);
		SqlExecutionService sqlExecutionService = new SqlExecutionService(dslContext, ResultSetProcessorFactory.create(sqlDialect));
		ExecutionManager<SqlExecutionResult> executionManager = new SqlExecutionManager(sqlConverter, sqlExecutionService, metaStorage);
		SqlStorageHandler sqlStorageHandler = new SqlStorageHandler(sqlExecutionService);

		return new LocalNamespace(
				namespaceData.getPreprocessMapper(),
				namespaceData.getCommunicationMapper(),
				namespaceStorage,
				executionManager,
				dslContextWrapper,
				sqlStorageHandler,
				namespaceData.getJobManager(),
				namespaceData.getFilterSearch(),
				namespaceData.getIndexService(),
				namespaceData.getInjectables()
		);
	}

	@Override
	public void removeNamespace(DatasetId id, LocalNamespace namespace) {
		// nothing to do
	}

}
