package com.bakdata.conquery.mode.cluster;

import java.io.InputStream;
import java.util.Collection;

import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.jobs.ImportJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * Handler of {@link Import} requests that realizes them both on the manager and the cluster's shards.
 */
@AllArgsConstructor
public
class ClusterImportHandler implements ImportHandler {

	private final IdMutex<DictionaryId> sharedDictionaryLocks = new IdMutex<>();
	private final ConqueryConfig config;
	private final DatasetRegistry<DistributedNamespace> datasetRegistry;

	@SneakyThrows
	@Override
	public void updateImport(Namespace namespace, InputStream inputStream) {
		ImportJob job = ImportJob.createOrUpdate(
				datasetRegistry.get(namespace.getDataset().getId()),
				inputStream,
				config.getCluster().getEntityBucketSize(),
				sharedDictionaryLocks,
				config,
				true
		);

		namespace.getJobManager().addSlowJob(job);

		clearDependentConcepts(namespace.getStorage().getAllConcepts(), job.getTable());
	}

	@SneakyThrows
	@Override
	public void addImport(Namespace namespace, InputStream inputStream) {
		ImportJob job = ImportJob.createOrUpdate(
				datasetRegistry.get(namespace.getDataset().getId()),
				inputStream,
				config.getCluster().getEntityBucketSize(),
				sharedDictionaryLocks,
				config,
				false
		);
		namespace.getJobManager().addSlowJob(job);

		clearDependentConcepts(namespace.getStorage().getAllConcepts(), job.getTable());
	}

	@Override
	public void deleteImport(Import imp) {

		DatasetId id = imp.getTable().getDataset().getId();
		final DistributedNamespace namespace = datasetRegistry.get(id);

		clearDependentConcepts(namespace.getStorage().getAllConcepts(), imp.getTable());

		namespace.getStorage().removeImport(imp.getId());
		namespace.getWorkerHandler().sendToAll(new RemoveImportJob(imp));

		// Remove bucket assignments for consistency report
		namespace.getWorkerHandler().removeBucketAssignmentsForImportFormWorkers(imp);
	}

	private void clearDependentConcepts(Collection<Concept<?>> allConcepts, Table table) {
		for (Concept<?> c : allConcepts) {
			for (Connector con : c.getConnectors()) {
				if (!con.getTable().equals(table)) {
					continue;
				}

				con.getConcept().clearMatchingStats();
			}
		}
	}
}
