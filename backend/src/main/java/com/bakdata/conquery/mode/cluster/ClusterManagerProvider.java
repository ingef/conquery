package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.ClusterHealthCheck;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.tasks.ReportConsistencyTask;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.tasks.Task;

public class ClusterManagerProvider implements ManagerProvider {

	public ClusterManager provideManager(ConqueryConfig config, Environment environment) {
		final JobManager jobManager = ManagerProvider.newJobManager(config);
		final MetaStorage storage = new MetaStorage(config.getStorage());
		final InternalMapperFactory internalMapperFactory = new InternalMapperFactory(config, environment.getValidator());
		final ClusterState clusterState = new ClusterState();
		final NamespaceHandler<DistributedNamespace> namespaceHandler = new ClusterNamespaceHandler(clusterState, config, internalMapperFactory);
		final DatasetRegistry<DistributedNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(namespaceHandler, config, internalMapperFactory);

		final ClusterConnectionManager connectionManager =
				new ClusterConnectionManager(datasetRegistry, jobManager, environment.getValidator(), config, internalMapperFactory, clusterState);

		final ImportHandler importHandler = new ClusterImportHandler(datasetRegistry);
		final StorageListener extension = new ClusterStorageListener(jobManager, datasetRegistry);
		final Supplier<Collection<ShardNodeInformation>> nodeProvider = () -> clusterState.getShardNodes().values();
		final List<Task> adminTasks = List.of(new ReportConsistencyTask(clusterState));

		final DelegateManager<DistributedNamespace>
				delegate =
				new DelegateManager<>(config, environment, datasetRegistry, storage, importHandler, extension, nodeProvider, adminTasks, internalMapperFactory, jobManager);

		environment.healthChecks()
				   .register("cluster", new ClusterHealthCheck(clusterState, nodeProvider, config.getCluster().getHeartbeatTimeout().toJavaDuration()));

		return new ClusterManager(delegate, connectionManager);
	}


}
