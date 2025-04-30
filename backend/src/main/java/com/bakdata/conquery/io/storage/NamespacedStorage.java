package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.SneakyThrows;

public interface NamespacedStorage extends ConqueryStorage, NamespacedStorageProvider {
	void addImport(Import imp);

	Import getImport(ImportId id);

	Stream<ImportId> getAllImports();

	Stream<ImportId> getAllImportIds();

	void updateImport(Import imp);

	void removeImport(ImportId id);

	void updateDataset(Dataset dataset);

	Table getTable(TableId tableId);

	Stream<Table> getTables();

	void addTable(Table table);

	void removeTable(TableId table);

	SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId);

	Stream<SecondaryIdDescription> getSecondaryIds();

	void addSecondaryId(SecondaryIdDescription secondaryIdDescription);

	void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId);

	Concept<?> getConcept(ConceptId id);

	Stream<Concept<?>> getAllConcepts();

	boolean hasConcept(ConceptId id);

	@SneakyThrows
	void updateConcept(Concept<?> concept);

	void removeConcept(ConceptId id);

	@Override
	default NamespacedStorage getStorage(DatasetId datasetId) {
		if (getDataset() == null || datasetId.getName().equals(getDataset().getName())) {
			// Storage was empty (new Worker/Namespace) or it matches
			return this;
		}
		return null;
	}

	Dataset getDataset();
}
