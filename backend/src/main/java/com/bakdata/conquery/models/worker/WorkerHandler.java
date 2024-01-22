package com.bakdata.conquery.models.worker;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for worker in a single namespace.
 */
@Slf4j
@RequiredArgsConstructor
public class WorkerHandler {

	private final ObjectMapper communicationMapper;
	/**
	 * All known {@link Worker}s that are part of this Namespace.
	 */
	private final IdMap<WorkerId, WorkerInformation> workers = new IdMap<>();

	/**
	 * Map storing the buckets each Worker has been assigned.
	 */
	private final Int2ObjectMap<WorkerInformation> bucket2WorkerMap = new Int2ObjectArrayMap<>();

	private final NamespaceStorage storage;

	public IdMap<WorkerId, WorkerInformation> getWorkers() {
		return this.workers;
	}

	public void sendToAll(WorkerMessage msg) {
		if (workers.isEmpty()) {
			throw new IllegalStateException("There are no workers yet");
		}
		for (WorkerInformation w : workers.values()) {
			w.send(msg);
		}
	}

	public synchronized void removeBucketAssignmentsForImportFormWorkers(@NonNull Import importId) {
		final WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null) {
			return;
		}
		workerBuckets.removeBucketsOfImport(importId.getId());

		storage.setWorkerToBucketsMap(workerBuckets);

		sendUpdatedWorkerInformation();
	}

	private synchronized void sendUpdatedWorkerInformation() {
		for (WorkerInformation w : this.workers.values()) {
			w.send(new UpdateWorkerBucket(w));
		}
	}

	private synchronized WorkerToBucketsMap createWorkerBucketsMap() {
		// Ensure that only one map is created and populated in the storage
		WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets != null) {
			return workerBuckets;
		}
		storage.setWorkerToBucketsMap(new WorkerToBucketsMap());
		return storage.getWorkerBuckets();
	}

	public synchronized void addBucketsToWorker(@NonNull WorkerId id, @NonNull Set<BucketId> bucketIds) {
		// Ensure that add and remove are not executed at the same time.
		// We don't make assumptions about the underlying implementation regarding thread safety
		WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null) {
			workerBuckets = createWorkerBucketsMap();
		}
		workerBuckets.addBucketForWorker(id, bucketIds);

		storage.setWorkerToBucketsMap(workerBuckets);

		sendUpdatedWorkerInformation();
	}

	public synchronized WorkerInformation getResponsibleWorkerForBucket(int bucket) {
		return bucket2WorkerMap.get(bucket);
	}

	/**
	 * @implNote Currently the least occupied Worker receives a new Bucket, this can change in later implementations. (For example for
	 * 	dedicated Workers, or entity weightings)
	 */

	public synchronized void addResponsibility(int bucket) {
		WorkerInformation smallest = workers
				.stream()
				.min(Comparator.comparing(si -> si.getIncludedBuckets().size()))
				.orElseThrow(() -> new IllegalStateException("Unable to find minimum."));

		log.debug("Assigning Bucket[{}] to Worker[{}]", bucket, smallest.getId());

		bucket2WorkerMap.put(bucket, smallest);

		smallest.getIncludedBuckets().add(bucket);
	}

	public synchronized void addWorker(WorkerInformation info) {
		Objects.requireNonNull(info.getConnectedShardNode(), () -> String.format("No open connections found for Worker[%s]", info.getId()));

		info.setCommunicationWriter(communicationMapper.writer());

		workers.add(info);

		for (Integer bucket : info.getIncludedBuckets()) {
			final WorkerInformation old = bucket2WorkerMap.put(bucket.intValue(), info);

			// This is a completely invalid state from which we should not recover even in production settings.
			if (old != null && !old.equals(info)) {
				throw new IllegalStateException(String.format("Duplicate claims for Bucket[%d] from %s and %s", bucket, old, info));
			}
		}
	}

	public void register(ShardNodeInformation node, WorkerInformation info) {
		WorkerInformation old = this.getWorkers().getOptional(info.getId()).orElse(null);
		if (old != null) {
			old.setIncludedBuckets(info.getIncludedBuckets());
			old.setConnectedShardNode(node);
		}
		else {
			info.setConnectedShardNode(node);
		}
		this.addWorker(info);
	}

	public Set<BucketId> getBucketsForWorker(WorkerId workerId) {
		final WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null) {
			return Collections.emptySet();
		}
		return workerBuckets.getBucketsForWorker(workerId);
	}
}
