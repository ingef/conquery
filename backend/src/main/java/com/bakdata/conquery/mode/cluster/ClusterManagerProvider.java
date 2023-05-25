package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.tasks.ReportConsistencyTask;
import io.dropwizard.setup.Environment;

public class ClusterManagerProvider implements ManagerProvider {

	public ClusterManager provideManager(ConqueryConfig config, Environment environment) {
		JobManager jobManager = ManagerProvider.newJobManager(config);
		InternalObjectMapperCreator creator = ManagerProvider.newInternalObjectMapperCreator(config, environment.getValidator());
		ClusterState clusterState = new ClusterState();
		NamespaceHandler<DistributedNamespace> namespaceHandler = new ClusterNamespaceHandler(clusterState, config, creator);
		DatasetRegistry<DistributedNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(namespaceHandler, config, creator);
		creator.init(datasetRegistry);

		ClusterConnectionManager connectionManager = new ClusterConnectionManager(
				datasetRegistry, jobManager, environment.getValidator(), config, creator, clusterState
		);
		ImportHandler importHandler = new ClusterImportHandler(config, datasetRegistry);
		StorageListener extension = new ClusterStorageListener(jobManager, datasetRegistry);
		Supplier<Collection<ShardNodeInformation>> nodeProvider = () -> clusterState.getShardNodes().values();

		DelegateManager<DistributedNamespace> delegate = new DelegateManager<>(
				config,
				environment,
				datasetRegistry,
				importHandler,
				extension,
				nodeProvider,
				List.of(new ReportConsistencyTask(clusterState)),
				creator,
				jobManager
		);

		return new ClusterManager(delegate, connectionManager);
	}


}
