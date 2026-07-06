package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

public interface NamespaceStorage {

	DatasetCatalogRepository.DatasetRecord dataset();

	List<DatasetCatalogRepository.Concept> listConcepts();

	Optional<DatasetCatalogRepository.Concept> findConcept(String conceptId);

	void saveConcept(DatasetCatalogRepository.Concept concept);

	boolean deleteConcept(String conceptId);

	List<DatasetCatalogRepository.TableRecord> listTables();

	Optional<DatasetCatalogRepository.TableRecord> findTable(String tableId);

	void saveTable(DatasetCatalogRepository.TableRecord table);

	boolean deleteTable(String tableId);
}
