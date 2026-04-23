package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

public interface NamespaceStorage {

	DatasetCatalogRepository.DatasetRecord dataset();

	List<DatasetCatalogRepository.ConceptRecord> listConcepts();

	Optional<DatasetCatalogRepository.ConceptRecord> findConcept(String conceptId);

	void saveConcept(DatasetCatalogRepository.ConceptRecord concept);

	boolean deleteConcept(String conceptId);

	List<DatasetCatalogRepository.TableRecord> listTables();

	Optional<DatasetCatalogRepository.TableRecord> findTable(String tableId);

	void saveTable(DatasetCatalogRepository.TableRecord table);

	boolean deleteTable(String tableId);
}
