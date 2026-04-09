package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

public interface DatasetCatalogRepository {

	List<DatasetRecord> listDatasets();

	Optional<DatasetRecord> findDataset(String datasetId);

	List<ConceptRecord> listConcepts();

	Optional<ConceptRecord> findConcept(String conceptId);

	record DatasetRecord(
			String id,
			String label
	) {
	}

	record ConceptRecord(
			String id,
			String label,
			String datasetId
	) {
	}
}
