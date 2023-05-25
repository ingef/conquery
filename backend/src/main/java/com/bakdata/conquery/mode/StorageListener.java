package com.bakdata.conquery.mode;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;

/**
 * Listener for updates of stored entities in ConQuery.
 */
public interface StorageListener {

	default void onAddSecondaryId(SecondaryIdDescription secondaryId) {
	}

	default void onDeleteSecondaryId(SecondaryIdDescription description) {
	}

	default void onAddTable(Table table) {
	}

	default void onRemoveTable(Table table) {
	}

	default void onAddConcept(Concept<?> concept) {
	}

	default void onDeleteConcept(Concept<?> concept) {
	}

	default void onUpdateMatchingStats(final Dataset dataset) {
	}

}
