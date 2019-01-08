package com.bakdata.conquery.models.events;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;

public interface BlockManager {

	default void init() {}

	default void addImport(Import imp) {}

	default void addConcept(Concept<?> c) {}

	default void removeImport(ImportId imp) {}

	default void removeConcept(ConceptId conceptId) {}

}