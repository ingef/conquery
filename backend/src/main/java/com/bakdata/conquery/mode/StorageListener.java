package com.bakdata.conquery.mode;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;

/**
 * Listener for updates of stored entities in ConQuery.
 */
public interface StorageListener {

	void onAddSecondaryId(SecondaryIdDescription secondaryId);

	void onDeleteSecondaryId(SecondaryIdDescriptionId description);

	void onAddTable(Table table);

	void onRemoveTable(TableId table);

	void onAddConcept(Concept<?> concept);

	void onDeleteConcept(ConceptId concept);

}
