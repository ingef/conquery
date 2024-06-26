package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.*;
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
		final ClusterState clusterState = new ClusterState();
		final NamespaceHandler<DistributedNamespace> namespaceHandler = new ClusterNamespaceHandler(clusterState, config);
		final InternalObjectMapperCreator creator = ManagerProvider.newInternalObjectMapperCreator(config, environment.getValidator());
		final DatasetRegistry<DistributedNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(namespaceHandler, config, creator);
		final MetaStorage metaStorage = ManagerProvider.createMetaStorage(config.getStorage());

		final ClusterConnectionManager connectionManager =
				new ClusterConnectionManager(datasetRegistry, jobManager, environment.getValidator(), config, creator, clusterState);

		final ImportHandler importHandler = new ClusterImportHandler(config, datasetRegistry);
		final StorageListener extension = new ClusterStorageListener(jobManager, datasetRegistry);
		final Supplier<Collection<ShardNodeInformation>> nodeProvider = () -> clusterState.getShardNodes().values();
		final List<Task> adminTasks = List.of(new ReportConsistencyTask(clusterState));

		final DelegateManager<DistributedNamespace>
				delegate =
				new DelegateManager<>(config, environment, datasetRegistry, metaStorage, importHandler, extension, nodeProvider, adminTasks, creator, jobManager);

		environment.healthChecks().register("cluster", new ClusterHealthCheck(clusterState));

		return new ClusterManager(delegate, connectionManager);
	}


}
