package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conquery.SqlExecutionManager;
import com.bakdata.conquery.sql.conversion.NodeConversions;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialectFactory;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.ResultSetProcessorFactory;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import io.dropwizard.core.setup.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Slf4j
public class LocalNamespaceHandler implements NamespaceHandler<LocalNamespace> {

	private final ConqueryConfig config;
	private final InternalMapperFactory internalMapperFactory;
	private final SqlDialectFactory dialectFactory;

	@Override
	public LocalNamespace createNamespace(NamespaceStorage namespaceStorage, MetaStorage metaStorage, DatasetRegistry<LocalNamespace> datasetRegistry, Environment environment) {

		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(namespaceStorage, config, internalMapperFactory, datasetRegistry, environment);

		IdColumnConfig idColumns = config.getIdColumns();
		SqlConnectorConfig sqlConnectorConfig = config.getSqlConnectorConfig();
		DatabaseConfig databaseConfig = sqlConnectorConfig.getDatabaseConfig(namespaceStorage.getDataset());

		DSLContextWrapper dslContextWrapper = DslContextFactory.create(databaseConfig, sqlConnectorConfig, environment.healthChecks());
		DSLContext dslContext = dslContextWrapper.getDslContext();
		SqlDialect sqlDialect = dialectFactory.createSqlDialect(databaseConfig.getDialect());

		boolean valid = dslContext.connectionResult(connection -> connection.isValid(1));

		if (!valid) {
			throw new IllegalStateException("Unable to connect to %s".formatted(databaseConfig));
		}

		ResultSetProcessor resultSetProcessor = ResultSetProcessorFactory.create(config, sqlDialect);
		SqlExecutionService sqlExecutionService = new SqlExecutionService(dslContext, resultSetProcessor);
		NodeConversions nodeConversions = new NodeConversions(idColumns, sqlDialect, dslContext, databaseConfig, sqlExecutionService);
		SqlConverter sqlConverter = new SqlConverter(nodeConversions, config);
		ExecutionManager executionManager = new SqlExecutionManager(sqlConverter, sqlExecutionService, metaStorage, datasetRegistry, config);
		SqlStorageHandler sqlStorageHandler = new SqlStorageHandler(sqlExecutionService);
		SqlEntityResolver sqlEntityResolver = new SqlEntityResolver(idColumns, dslContext, sqlDialect, sqlExecutionService);

		return new LocalNamespace(
				sqlDialect,
				namespaceData.preprocessMapper(),
				namespaceStorage,
				executionManager,
				dslContextWrapper,
				sqlStorageHandler,
				namespaceData.jobManager(),
				namespaceData.filterSearch(),
				sqlEntityResolver
		);
	}

	@Override
	public void removeNamespace(DatasetId id, LocalNamespace namespace) {
		// nothing to do
	}

}
