package com.bakdata.conquery.models.worker;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.ReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateWorkerBucket;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for worker in a single namespace.
 */
@Slf4j
@RequiredArgsConstructor
public class WorkerHandler {

	/**
	 * All known {@link Worker}s that are part of this Namespace.
	 */
	@Getter
	private final IdMap<WorkerId, WorkerInformation> workers = new IdMap<>();

	/**
	 * Map storing the buckets each Worker has been assigned.
	 */
	private final Int2ObjectMap<WorkerInformation> bucket2WorkerMap = new Int2ObjectArrayMap<>();

	private final NamespaceStorage storage;

	private final Map<UUID, PendingReaction> pendingReactions = new HashMap<>();

	@NotNull
	public Set<WorkerId> getAllWorkerIds() {
		return getWorkers().stream()
						   .map(WorkerInformation::getId)
						   .collect(Collectors.toSet());
	}


	public void sendToAll(WorkerMessage msg) {
		if (workers.isEmpty()) {
			throw new IllegalStateException("There are no workers yet");
		}

		// Register tracker for pending reactions if applicable
		if (msg instanceof ActionReactionMessage actionReactionMessage) {
			final UUID callerId = actionReactionMessage.getMessageId();
			pendingReactions.put(callerId, new PendingReaction(callerId, new HashSet<>(workers.keySet()), actionReactionMessage));
		}

		// Send message to all workers
		for (WorkerInformation w : workers.values()) {
			w.send(msg);
		}
	}

	public void handleReactionMessage(ReactionMessage message) {
		final UUID callerId = message.getCallerId();
		final PendingReaction pendingReaction = pendingReactions.get(callerId);

		if (pendingReaction == null) {
			throw new IllegalStateException(String.format("No pending action registered (anymore) for caller id %s from reaction message: %s", callerId, message));
		}

		if (pendingReaction.checkoffWorker(message)) {
			log.debug("Removing pending reaction '{}' as last pending message was received.", callerId);
			pendingReactions.remove(callerId);
		}

	}

	public synchronized void removeBucketAssignmentsForImportFormWorkers(ImportId importId) {
		final WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null) {
			return;
		}
		workerBuckets.removeBucketsOfImport(importId);

		storage.setWorkerToBucketsMap(workerBuckets);

		sendUpdatedWorkerInformation();
	}

	public synchronized void sendUpdatedWorkerInformation() {
		for (WorkerInformation w : workers.values()) {
			w.send(new UpdateWorkerBucket(w));
		}
	}

	public synchronized void registerBucketForWorker(@NonNull WorkerId id, @NonNull BucketId bucketId) {
		// Ensure that add and remove are not executed at the same time.
		// We don't make assumptions about the underlying implementation regarding thread safety
		WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();

		if (workerBuckets == null) {
			workerBuckets = createWorkerBucketsMap();
		}

		workerBuckets.addBucketForWorker(id, bucketId);

		storage.setWorkerToBucketsMap(workerBuckets);
	}

	private synchronized WorkerToBucketsMap createWorkerBucketsMap() {
		// Ensure that only one map is created and populated in the storage
		final WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets != null) {
			return workerBuckets;
		}
		storage.setWorkerToBucketsMap(new WorkerToBucketsMap());
		return storage.getWorkerBuckets();
	}

	public synchronized WorkerInformation getResponsibleWorkerForBucket(int bucket) {
		return bucket2WorkerMap.get(bucket);
	}

	/**
	 * @return
	 * @implNote Currently the least occupied Worker receives a new Bucket, this can change in later implementations. (For example for
	 * dedicated Workers, or entity weightings)
	 */

	public synchronized WorkerInformation addResponsibility(int bucket) {
		final WorkerInformation smallest = workers
				.stream()
				.min(Comparator.comparing(si -> si.getIncludedBuckets().size()))
				.orElseThrow(() -> new IllegalStateException("Unable to find minimum."));

		log.debug("Assigning Bucket[{}] to Worker[{}]", bucket, smallest.getId());

		bucket2WorkerMap.put(bucket, smallest);

		smallest.getIncludedBuckets().add(bucket);

		return smallest;
	}

	public void register(ShardNodeInformation node, WorkerInformation info) {
		final WorkerInformation old = getWorkers().getOptional(info.getId()).orElse(null);
		if (old != null) {
			old.setIncludedBuckets(info.getIncludedBuckets());
			old.setConnectedShardNode(node);
		}
		else {
			info.setConnectedShardNode(node);
		}
		addWorker(info);
	}

	public synchronized void addWorker(WorkerInformation info) {
		Objects.requireNonNull(info.getConnectedShardNode(), () -> String.format("No open connections found for Worker[%s]", info.getId()));

		workers.add(info);

		for (Integer bucket : info.getIncludedBuckets()) {
			final WorkerInformation old = bucket2WorkerMap.put(bucket.intValue(), info);

			// This is a completely invalid state from which we should not recover even in production settings.
			if (old != null && !old.equals(info)) {
				throw new IllegalStateException(String.format("Duplicate claims for Bucket[%d] from %s and %s", bucket, old, info));
			}
		}
	}

	public Set<BucketId> getBucketsForWorker(WorkerId workerId) {
		final WorkerToBucketsMap workerBuckets = storage.getWorkerBuckets();
		if (workerBuckets == null) {
			return Collections.emptySet();
		}
		return workerBuckets.getBucketsForWorker(workerId);
	}

	public synchronized WorkerInformation assignResponsibleWorker(BucketId bucket) {

		WorkerInformation responsibleWorkerForBucket = getResponsibleWorkerForBucket(bucket.getBucket());

		if (responsibleWorkerForBucket == null) {
			responsibleWorkerForBucket = addResponsibility(bucket.getBucket());
		}

		registerBucketForWorker(responsibleWorkerForBucket.getId(), bucket);

		return responsibleWorkerForBucket;
	}

	public boolean hasPendingMessages() {
		return !pendingReactions.isEmpty();
	}

	private record PendingReaction(UUID callerId, Set<WorkerId> pendingWorkers, ActionReactionMessage parent) {

		/**
		 * Marks the given worker as not pending. If the last pending worker checks off the afterAllReaction is executed.
		 */
		public synchronized boolean checkoffWorker(ReactionMessage message) {
			final WorkerId workerId = message.getWorkerId();

			if (!message.lastMessageFromWorker()) {
				log.trace("Received reacting message, but was not the last one: {}", message);
				return false;
			}

			if (!pendingWorkers.remove(workerId)) {
				throw new IllegalStateException(String.format("Could not check off worker %s for action-reaction message '%s'. Worker was not checked in.", workerId, callerId));
			}

			log.debug("Checked off worker '{}' for action-reaction message '{}', still waiting for {}.", workerId, parent, pendingWorkers.size());

			if (!pendingWorkers.isEmpty()) {
				return false;
			}

			log.debug("Checked off last worker '{}' for action-reaction message {}. Calling hook", workerId, parent);

			parent.afterAllReaction();
			return true;

		}
	}
}
