package com.bakdata.conquery.models.worker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * Keep track of all data assigned to a single dataset. Each Slave has one {@link Worker} per {@link Dataset} / {@link Namespace}.
 * Every Worker is assigned a partition of the loaded {@link Entity}s via {@link Entity::getBucket}.
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
public class Namespace {

	@JsonIgnore
	private transient NamespaceStorage storage;
	@JsonIgnore
	private transient ExecutionManager queryManager;

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
	private transient Namespaces namespaces;

	public Namespace(NamespaceStorage storage) {
		this.storage = storage;
		this.queryManager = new ExecutionManager(this);
	}

	public void initMaintenance(ScheduledExecutorService maintenanceService) {
	}

	public void checkConnections() {
		List<WorkerInformation> l = new ArrayList<>(workers);
		l.removeIf(w -> w.getConnectedSlave() != null);

		if (!l.isEmpty()) {
			throw new IllegalStateException("Not all known slaves are connected. Missing " + l);
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
		Objects.requireNonNull(info.getConnectedSlave(), () -> String.format("No open connections found for Worker[%s]", info.getId()));

		Set<WorkerInformation> l = new HashSet<>(workers);
		l.add(info);
		workers = l;

		for (Integer bucket : info.getIncludedBuckets()) {
			final WorkerInformation old = bucket2WorkerMap.put(bucket.intValue(), info);

			if (old != null && !old.equals(info)) {
				log.error("Duplicate claims for Bucket[{}] from {} and {}", bucket, old, info);
			}
		}
	}

	@JsonIgnore
	public Dataset getDataset() {
		return storage.getDataset();
	}
}
