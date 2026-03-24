package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.conquery.SqlExecutionManager;
import com.bakdata.conquery.sql.conversion.NodeConversions;
import com.bakdata.conquery.sql.conversion.SqlConverter;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
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
	private final ConnectionManager connectionManager;
	private final DateNowSupplier dateNowSupplier;

	@Override
	public LocalNamespace createNamespace(
			NamespaceStorage namespaceStorage,
			MetaStorage metaStorage,
			DatasetRegistry<LocalNamespace> datasetRegistry,
			Environment environment) {

		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(namespaceStorage, config, internalMapperFactory, datasetRegistry, environment);

		ManagedConnection connection = connectionManager.getConnection(namespaceStorage.getDataset());
		DSLContext dslContext = connection.connect();
		DialectBundle dialectBundle = connection.getConnection().getDialect().getDialectBundle();

		ResultSetProcessor resultSetProcessor = ResultSetProcessorFactory.create(config, dialectBundle);
		SqlExecutionService sqlExecutionService = new SqlExecutionService(dslContext, resultSetProcessor);

		NodeConversions nodeConversions = new NodeConversions(config.getIdColumns(), dialectBundle, dslContext, sqlExecutionService, dateNowSupplier);
		SqlConverter sqlConverter = new SqlConverter(nodeConversions, config);
		ExecutionManager executionManager = new SqlExecutionManager(sqlConverter, sqlExecutionService, metaStorage, datasetRegistry, config);
		SqlStorageHandler sqlStorageHandler = new SqlStorageHandler(sqlExecutionService);
		SqlEntityResolver sqlEntityResolver = new SqlEntityResolver(config.getIdColumns(), dslContext, dialectBundle, sqlExecutionService);

		return new LocalNamespace(
				dialectBundle,
				namespaceData.preprocessMapper(),
				namespaceStorage,
				executionManager,
				dslContext, sqlStorageHandler,
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
