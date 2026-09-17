package com.bakdata.conquery.mode.cluster;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.models.worker.WorkerHandler;
import io.dropwizard.core.setup.Environment;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClusterNamespaceHandler implements NamespaceHandler<DistributedNamespace> {
	private final ClusterState clusterState;
	private final ConqueryConfig config;
	private final InternalMapperFactory internalMapperFactory;

	@Override
	public DistributedNamespace createNamespace(
			NamespaceStorage namespaceStorage,
			MetaStorage metaStorage,
			DatasetRegistry<DistributedNamespace> datasetRegistry,
			Environment environment) {

		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(namespaceStorage, config, internalMapperFactory, datasetRegistry, environment);
		DistributedExecutionManager executionManager = new DistributedExecutionManager(metaStorage, datasetRegistry, clusterState, config);
		WorkerHandler workerHandler = new WorkerHandler(namespaceStorage);
		clusterState.getWorkerHandlers().put(namespaceStorage.getDataset().getId(), workerHandler);

		DistributedNamespace distributedNamespace = new DistributedNamespace(
				namespaceData.preprocessMapper(),
				namespaceStorage,
				executionManager,
				namespaceData.jobManager(),
				namespaceData.filterSearch(),
				new ClusterEntityResolver(),
				workerHandler,
				config.getCluster()
		);

		for (ShardNodeInformation node : clusterState.getShardNodes().values()) {
			node.send(new AddWorker(namespaceStorage.getDataset()));
		}
		return distributedNamespace;
	}


	@Override
	public void removeNamespace(DatasetId id, DistributedNamespace namespace) {
		clusterState.getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(namespace.getDataset().getId())));
		clusterState.getWorkerHandlers().keySet().removeIf(worker -> worker.getDataset().getDataset().equals(id));
	}

}
