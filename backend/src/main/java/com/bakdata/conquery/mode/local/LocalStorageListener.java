package com.bakdata.conquery.mode.local;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import lombok.Data;

@Data
public class LocalStorageListener implements StorageListener {

	private final DatasetRegistry<LocalNamespace> datasetRegistry;

	@Override
	public void onAddSecondaryId(SecondaryIdDescription secondaryId) {
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescriptionId description) {
	}

	@Override
	public void onAddTable(Table table) {

	}

	@Override
	public void onRemoveTable(TableId table) {
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
	}

	@Override
	public void onDeleteConcept(ConceptId concept) {
	}
}
