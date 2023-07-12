package com.bakdata.conquery.mode.cluster;

import java.net.SocketAddress;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.models.worker.WorkerHandler;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.Value;

@Value
public class ClusterState {
	ConcurrentMap<SocketAddress, ShardNodeInformation> shardNodes = new ConcurrentHashMap<>();
	ConcurrentMap<DatasetId, WorkerHandler> workerHandlers = new ConcurrentHashMap<>();

	public synchronized void register(ShardNodeInformation node, WorkerInformation info) {
		WorkerHandler workerHandler = workerHandlers.get(info.getDataset());
		if (workerHandler == null) {
			throw new NoSuchElementException("Trying to register a worker for unknown dataset '%s'. I only know %s".formatted(info.getDataset(), workerHandlers.keySet()));
		}
		workerHandler.register(node, info);
	}

	public WorkerInformation getWorker(final WorkerId workerId, final DatasetId id) {
		return Optional.ofNullable(workerHandlers.get(id))
					   .flatMap(ns -> ns.getWorkers().getOptional(workerId))
					   .orElseThrow(() -> new NoSuchElementException("Unknown worker worker '%s' for dataset '%s'".formatted(workerId, id)));
	}

}
