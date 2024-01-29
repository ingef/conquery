package com.bakdata.conquery.mode.local;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.SqlContext;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.execution.ResultSetProcessorFactory;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import io.dropwizard.setup.Environment;
import org.jooq.DSLContext;

public class LocalManagerProvider implements ManagerProvider {

	private static final Supplier<Collection<ShardNodeInformation>> EMPTY_NODE_PROVIDER = Collections::emptyList;

	public DelegateManager<LocalNamespace> provideManager(ConqueryConfig config, Environment environment) {

		InternalObjectMapperCreator creator = ManagerProvider.newInternalObjectMapperCreator(config, environment.getValidator());

		SqlConnectorConfig sqlConnectorConfig = config.getSqlConnectorConfig();
		DSLContext dslContext = DslContextFactory.create(sqlConnectorConfig);
		SqlDialect sqlDialect = createSqlDialect(sqlConnectorConfig, dslContext);
		SqlContext sqlContext = new SqlContext(sqlConnectorConfig, sqlDialect);

		SqlExecutionService sqlExecutionService = new SqlExecutionService(
				sqlDialect.getDSLContext(),
				ResultSetProcessorFactory.create(sqlDialect)
		);

		NamespaceHandler<LocalNamespace> namespaceHandler = new LocalNamespaceHandler(config, creator, sqlContext, sqlExecutionService);
		DatasetRegistry<LocalNamespace> datasetRegistry = ManagerProvider.createLocalDatasetRegistry(namespaceHandler, config, creator, sqlExecutionService);
		creator.init(datasetRegistry);

		return new DelegateManager<>(
				config,
				environment,
				datasetRegistry,
				new FailingImportHandler(),
				new LocalStorageListener(),
				EMPTY_NODE_PROVIDER,
				List.of(),
				creator,
				ManagerProvider.newJobManager(config)
		);
	}

	protected SqlDialect createSqlDialect(SqlConnectorConfig sqlConnectorConfig, DSLContext dslContext) {
		return switch (sqlConnectorConfig.getDialect()) {
			case POSTGRESQL -> new PostgreSqlDialect(dslContext);
			case HANA -> new HanaSqlDialect(dslContext);
		};
	}
}
