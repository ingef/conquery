package com.bakdata.conquery.models.worker;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Keep track of all data assigned to a single dataset. Each ShardNode has one {@link Worker} per {@link Dataset} / {@link Namespace}.
 * Every Worker is assigned a partition of the loaded {@link Entity}s via {@link Entity::getBucket}.
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
public class Namespace implements Closeable {

	@JsonIgnore
	private transient ObjectWriter objectWriter;
	@JsonIgnore
	@ToString.Include
	private transient NamespaceStorage storage;

	@JsonIgnore
	private transient ExecutionManager executionManager;

	// TODO: 01.07.2020 FK: This is not used a lot, as NamespacedMessages are highly convoluted and hard to decouple as is.
	@JsonIgnore
	private transient JobManager jobManager;

	/**
	 * All known {@link Worker}s that are part of this Namespace.
	 */
	private Set<WorkerInformation> workers = new HashSet<>();

	/**
	 * Map storing the buckets each Worker has been assigned.
	 */
	@JsonIgnore
	private transient Int2ObjectMap<WorkerInformation> bucket2WorkerMap = new Int2ObjectArrayMap<>();

	@JsonIgnore
	private transient DatasetRegistry namespaces;

	public Namespace(NamespaceStorage storage, boolean failOnError, ObjectWriter objectWriter) {
		this.storage = storage;
		this.executionManager = new ExecutionManager(this);
		this.jobManager = new JobManager(storage.getDataset().getName(), failOnError);
		this.objectWriter = objectWriter;
	}

	public void checkConnections() {
		List<WorkerInformation> l = new ArrayList<>(workers);
		l.removeIf(w -> w.getConnectedShardNode() != null);

		if (!l.isEmpty()) {
			throw new IllegalStateException("Not all known ShardNodes are connected. Missing " + l);
		}
	}

	public void sendToAll(WorkerMessage msg) {
		if (workers.isEmpty()) {
			throw new IllegalStateException("There are no workers yet");
		}
		for (WorkerInformation w : workers) {
			w.send(msg);
		}
	}


	/**
	 * Find the assigned worker for the bucket. If there is none return null.
	 */
	public synchronized WorkerInformation getResponsibleWorkerForBucket(int bucket) {
		return bucket2WorkerMap.get(bucket);
	}

	/**
	 * Assign responsibility of a bucket to a Worker.
	 *
	 * @implNote Currently the least occupied Worker receives a new Bucket, this can change in later implementations. (For example for dedicated Workers, or entity weightings)
	 */
	public synchronized void addResponsibility(int bucket) {
		WorkerInformation smallest = workers.stream()
											.min(Comparator.comparing(si -> si.getIncludedBuckets().size()))
											.orElseThrow(() -> new IllegalStateException("Unable to find minimum."));

		log.debug("Assigning Bucket[{}] to Worker[{}]", bucket, smallest.getId());

		bucket2WorkerMap.put(bucket, smallest);

		smallest.getIncludedBuckets().add(bucket);
	}

	public synchronized void addWorker(WorkerInformation info) {
		Objects.requireNonNull(info.getConnectedShardNode(), () -> String.format("No open connections found for Worker[%s]", info.getId()));

		info.setObjectWriter(objectWriter);

		Set<WorkerInformation> l = new HashSet<>(workers);
		l.add(info);
		workers = l;

		for (Integer bucket : info.getIncludedBuckets()) {
			final WorkerInformation old = bucket2WorkerMap.put(bucket.intValue(), info);

			// This is a completely invalid state from which we should not recover even in production settings.
			if (old != null && !old.equals(info)) {
				throw new IllegalStateException(String.format("Duplicate claims for Bucket[%d] from %s and %s", bucket, old, info));
			}
		}
	}

	@JsonIgnore
	public Dataset getDataset() {
		return storage.getDataset();
	}
	
	public void close() {
		try {
			jobManager.close();
		}
		catch (Exception e) {
			log.error("Unable to close namespace jobmanager of {}", this, e);
		}
		
		try {
			log.info("Closing namespace storage of {}", getStorage().getDataset().getId());
			storage.close();
		}
		catch (IOException e) {
			log.error("Unable to close namespace storage of {}.", this, e);
		}
	}

	public void remove() {
		try {
			jobManager.close();
		}
		catch (Exception e) {
			log.error("Unable to close namespace jobmanager of {}", this, e);
		}

		log.info("Removing namespace storage of {}", getStorage().getDataset().getId());
		storage.removeStorage();
	}

	public Set<BucketId> getBucketsForWorker(WorkerId workerId) {
		return getWorkerBucketsMap().getBucketsForWorker(workerId);
	}

	private WorkerToBucketsMap getWorkerBucketsMap(){
		WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null){
			workerBuckets = createWorkerBucketsMap();
		}
		return workerBuckets;
	}

	private synchronized WorkerToBucketsMap createWorkerBucketsMap() {
		// Ensure that only one map is created and populated in the storage
		WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets != null){
			return workerBuckets;
		}
		storage.setWorkerToBucketsMap(new WorkerToBucketsMap());
		return storage.getWorkerBuckets();
	}

	public synchronized void addBucketsToWorker(@NonNull WorkerId id, @NonNull Set<BucketId> bucketIds) {
		// Ensure that add and remove are not executed at the same time.
		// We don't make assumptions about the underlying implementation regarding thread safety
		synchronized (this) {
			WorkerToBucketsMap map = getWorkerBucketsMap();
			map.addBucketForWorker(id, bucketIds);
			storage.setWorkerToBucketsMap(map);
		}
	}

	public synchronized void removeBucketAssignmentsForImportFormWorkers(@NonNull Import imp) {
		synchronized (this) {
			WorkerToBucketsMap map = getWorkerBucketsMap();
			map.removeBucketsOfImport(imp.getId());
			storage.setWorkerToBucketsMap(map);
		}
	}

	public int getNumberOfEntities() {
		return getStorage().getPrimaryDictionary().getSize();
	}
}
