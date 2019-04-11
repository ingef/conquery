package com.bakdata.conquery.models.worker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.QueryManager;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @NoArgsConstructor
public class Namespace {

	@JsonIgnore
	private transient NamespaceStorage storage;
	@JsonIgnore
	private transient QueryManager queryManager;
	private List<WorkerInformation> workers = new ArrayList<>();
	@JsonIgnore
	private transient List<WorkerInformation> bucket2WorkerMap = new ArrayList<>();
	@JsonIgnore
	private transient Namespaces namespaces;
	private int entityBucketSize;
	
	public Namespace(int entityBucketSize, NamespaceStorage storage) {
		this.entityBucketSize = entityBucketSize;
		this.storage = storage;
		this.queryManager = new QueryManager(this);
	}
	
	public void initMaintenance(ScheduledExecutorService maintenanceService) {
		queryManager.initMaintenance(maintenanceService);
	}
	
	public void checkConnections() {
		List<WorkerInformation> l = new ArrayList<>(workers);
		l.removeIf(w->w.getConnectedSlave()!=null);
			
		if(!l.isEmpty()) {
			throw new IllegalStateException("Not all known slaves are connected. Missing "+l);
		}
	}
	
	public void sendToAll(WorkerMessage msg) {
		if(workers.isEmpty()) {
			throw new IllegalStateException("There are no workers yet");
		}
		for(WorkerInformation w:workers) {
			w.send(msg);
		}
	}
	
	public synchronized void updateWorkerMap() {
		int maximumEntityId = workers
			.stream()
			.mapToInt(WorkerInformation::findLargestEntityId)
			.max()
			.orElse(-1);
		bucket2WorkerMap = new ArrayList<>(maximumEntityId+1);
		if(maximumEntityId >= 0) {
			for(int i=0;i<=maximumEntityId;i++) {
				bucket2WorkerMap.add(null);
			}
		}
			
		for(WorkerInformation wi:workers) {
			for(int i = wi.getIncludedBuckets().size()-1; i>=0; i--) {
				bucket2WorkerMap.set(wi.getIncludedBuckets().getInt(i), wi);
			}
		}
		
		for(int i = bucket2WorkerMap.size()-1; i>=0; i--) {
			if(bucket2WorkerMap.get(i) == null) {
				throw new IllegalStateException("The id "+i+" is not mapped to a slave although larger ones are");
			}
		}
	}
	
	@Nonnull
	public synchronized WorkerInformation getResponsibleWorker(int entityId) {
		int bucket = Entity.getBucket(entityId, entityBucketSize);
		if(bucket < bucket2WorkerMap.size()) {
			return bucket2WorkerMap.get(bucket);
		}
		else {
			return null;
		}
	}
	
	public synchronized void addResponsibility(int bucket) {
		WorkerInformation smallest = workers
				.stream()
				.min(Comparator.comparing(si->si.getIncludedBuckets().size()))
				.orElseThrow(() -> new IllegalStateException("Unable to find minimum."));
		smallest.getIncludedBuckets().add(bucket);
	}

	public synchronized void addWorker(WorkerInformation info) {
		Objects.requireNonNull(info.getConnectedSlave());
		List<WorkerInformation> l = new ArrayList<>(workers);
		l.add(info);
		workers = l;
	}
}
