package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;

public class LocalStorageListener implements StorageListener {

	// When running without shards, no further actions are required

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
	}
}
