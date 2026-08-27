package com.bakdata.conquery.mode.local;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import io.dropwizard.core.setup.Environment;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalManagerProvider implements ManagerProvider {

	private static final Supplier<Collection<ShardNodeInformation>> EMPTY_NODE_PROVIDER = Collections::emptyList;
	private final Clock clock;

	@Override
	public DelegateManager<LocalNamespace> provideManager(ConqueryConfig config, Environment environment) {

		final ConnectionManager connectionManager = config.getSqlConnectorConfig().toConnectionManager(environment);

		final JobManager jobManager = ManagerProvider.newJobManager(config);

		final MetaStorage storage = new MetaStorage(config.getStorage());
		final InternalMapperFactory internalMapperFactory = new InternalMapperFactory(
			config,
			environment.getValidator());
		final NamespaceHandler<LocalNamespace> namespaceHandler = new LocalNamespaceHandler(
			config,
			internalMapperFactory,
			connectionManager,
			clock);
		final DatasetRegistry<LocalNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(
			namespaceHandler,
			config,
			internalMapperFactory);

		return new DelegateManager<>(
			config,
			environment,
			datasetRegistry,
			storage,
			new FailingImportHandler(),
			new LocalStorageListener(jobManager, datasetRegistry),
			EMPTY_NODE_PROVIDER,
			List.of(),
			internalMapperFactory,
			ManagerProvider.newJobManager(config)
		);
	}

}
