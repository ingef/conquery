package com.bakdata.conquery.mode.cluster;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.NamespaceSetupData;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.query.DistributedExecutionManager;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.models.worker.WorkerHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClusterNamespaceHandler implements NamespaceHandler<DistributedNamespace> {
	private final ClusterState clusterState;
	private final ConqueryConfig config;
	private final InternalObjectMapperCreator mapperCreator;

	@Override
	public DistributedNamespace createNamespace(NamespaceStorage storage, final MetaStorage metaStorage) {
		NamespaceSetupData namespaceData = NamespaceHandler.createNamespaceSetup(storage, config, mapperCreator);
		DistributedExecutionManager executionManager = new DistributedExecutionManager(metaStorage, clusterState);
		WorkerHandler workerHandler = new WorkerHandler(namespaceData.getCommunicationMapper(), storage);
		clusterState.getWorkerHandlers().put(storage.getDataset().getId(), workerHandler);

		DistributedNamespace distributedNamespace = new DistributedNamespace(
				namespaceData.getPreprocessMapper(),
				namespaceData.getCommunicationMapper(),
				storage,
				executionManager,
				namespaceData.getJobManager(),
				namespaceData.getFilterSearch(),
				namespaceData.getIndexService(),
				namespaceData.getInjectables(),
				workerHandler
		);

		for (ShardNodeInformation node : clusterState.getShardNodes().values()) {
			node.send(new AddWorker(storage.getDataset()));
		}
		return distributedNamespace;
	}


	@Override
	public void removeNamespace(DatasetId id, DistributedNamespace namespace) {
		clusterState.getShardNodes().values().forEach(shardNode -> shardNode.send(new RemoveWorker(namespace.getDataset())));
		clusterState.getWorkerHandlers().keySet().removeIf(worker -> worker.getDataset().getDataset().equals(id));
	}

}
