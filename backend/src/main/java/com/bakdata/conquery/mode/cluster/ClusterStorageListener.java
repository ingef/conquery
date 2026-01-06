package com.bakdata.conquery.mode.cluster;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveTable;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateTable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
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
		datasetRegistry.get(secondaryId.getDataset()).getWorkerHandler().sendToAll(new UpdateSecondaryId(secondaryId));
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescriptionId secondaryId) {
		datasetRegistry.get(secondaryId.getDataset()).getWorkerHandler().sendToAll(new RemoveSecondaryId(secondaryId));
	}

	@Override
	public void onAddTable(Table table) {
		datasetRegistry.get(table.getDataset()).getWorkerHandler().sendToAll(new UpdateTable(table));
	}

	@Override
	public void onRemoveTable(TableId table) {
		datasetRegistry.get(table.getDataset()).getWorkerHandler().sendToAll(new RemoveTable(table));
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
		WorkerHandler handler = datasetRegistry.get(concept.getDataset()).getWorkerHandler();
		SimpleJob simpleJob = new SimpleJob(String.format("sendToAll : Add %s ", concept.getId()), () -> handler.sendToAll(new UpdateConcept(concept)));
		jobManager.addSlowJob(simpleJob);
	}

	@Override
	public void onDeleteConcept(ConceptId concept) {
		WorkerHandler handler = datasetRegistry.get(concept.getDataset()).getWorkerHandler();
		SimpleJob simpleJob = new SimpleJob("sendToAll: remove " + concept, () -> handler.sendToAll(new RemoveConcept(concept)));
		jobManager.addSlowJob(simpleJob);
	}
}
