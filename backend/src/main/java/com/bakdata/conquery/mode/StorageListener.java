package com.bakdata.conquery.mode;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;

/**
 * Listener for updates of stored entities in ConQuery.
 */
public interface StorageListener {

	void onAddSecondaryId(SecondaryIdDescription secondaryId);

	void onDeleteSecondaryId(SecondaryIdDescription description);

	void onAddTable(Table table);

	void onRemoveTable(Table table);

	void onAddConcept(Concept<?> concept);

	void onDeleteConcept(Concept<?> concept);

	void onUpdateMatchingStats(final Dataset dataset);

}
