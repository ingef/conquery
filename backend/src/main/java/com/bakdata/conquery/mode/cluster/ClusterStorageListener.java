package com.bakdata.conquery.mode.cluster;

import java.util.Collection;
import java.util.stream.Collectors;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveTable;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateTable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.WorkerHandler;
import lombok.AllArgsConstructor;

/**
 * Propagates changes of stored entities to relevant ConQuery shards in the cluster.
 */
@AllArgsConstructor
public
class ClusterStorageListener implements StorageListener {

	private final JobManager jobManager;
	private final DatasetRegistry<DistributedNamespace> datasetRegistry;

	@Override
	public void onAddSecondaryId(SecondaryIdDescription secondaryId) {
		datasetRegistry.get(secondaryId.getDataset().getId()).getWorkerHandler().sendToAll(new UpdateSecondaryId(secondaryId));
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescription secondaryId) {
		datasetRegistry.get(secondaryId.getDataset().getId()).getWorkerHandler().sendToAll(new RemoveSecondaryId(secondaryId));
	}

	@Override
	public void onAddTable(Table table) {
		datasetRegistry.get(table.getDataset().getId()).getWorkerHandler().sendToAll(new UpdateTable(table));
	}

	@Override
	public void onRemoveTable(Table table) {
		datasetRegistry.get(table.getDataset().getId()).getWorkerHandler().sendToAll(new RemoveTable(table));
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
		WorkerHandler handler = datasetRegistry.get(concept.getDataset().getId()).getWorkerHandler();
		SimpleJob simpleJob = new SimpleJob(String.format("sendToAll : Add %s ", concept.getId()), () -> handler.sendToAll(new UpdateConcept(concept)));
		jobManager.addSlowJob(simpleJob);
	}

	@Override
	public void onDeleteConcept(Concept<?> concept) {
		WorkerHandler handler = datasetRegistry.get(concept.getDataset().getId()).getWorkerHandler();
		SimpleJob simpleJob = new SimpleJob("sendToAll: remove " + concept.getId(), () -> handler.sendToAll(new RemoveConcept(concept)));
		jobManager.addSlowJob(simpleJob);
	}

	@Override
	public void onUpdateMatchingStats(final Dataset dataset) {
		final Namespace namespace = datasetRegistry.get(dataset.getId());
		final Collection<Concept<?>> concepts = namespace.getStorage().getAllConcepts()
														 .stream()
														 .filter(concept -> concept.getMatchingStats() == null)
														 .collect(Collectors.toSet());
		datasetRegistry.get(dataset.getId()).getWorkerHandler().sendToAll(new UpdateMatchingStatsMessage(concepts));
	}
}
