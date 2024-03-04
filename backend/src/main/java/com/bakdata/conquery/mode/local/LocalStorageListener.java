package com.bakdata.conquery.mode.local;

import java.util.Collection;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.jobs.SqlUpdateMatchingStatsJob;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalStorageListener implements StorageListener {

	private final DatasetRegistry<LocalNamespace> datasetRegistry;

	@Override
	public void onAddSecondaryId(SecondaryIdDescription secondaryId) {
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescription description) {
	}

	@Override
	public void onAddTable(Table table) {
	}

	@Override
	public void onRemoveTable(Table table) {
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
	}

	@Override
	public void onDeleteConcept(Concept<?> concept) {
	}

	@Override
	public void onUpdateMatchingStats(Dataset dataset) {

		final LocalNamespace namespace = datasetRegistry.get(dataset.getId());
		final Collection<Concept<?>> concepts = namespace.getStorage().getAllConcepts();

		SqlUpdateMatchingStatsJob matchingStatsJob = new SqlUpdateMatchingStatsJob(
				datasetRegistry.getConfig().getSqlConnectorConfig(),
				namespace.getSqlExecutionService(),
				namespace.getFunctionProvider(),
				concepts
		);

		datasetRegistry.get(dataset.getId()).getJobManager().addSlowJob(matchingStatsJob);
	}
}
