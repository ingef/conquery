package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.StructureNodeId;
import com.bakdata.conquery.quarkus.ids.TableId;

public interface NamespaceStorage {

	DatasetCatalogRepository.DatasetRecord dataset();

	List<DatasetCatalogRepository.Concept> listConcepts();

	Optional<DatasetCatalogRepository.Concept> findConcept(ConceptId conceptId);

	void saveConcept(DatasetCatalogRepository.Concept concept);

	boolean deleteConcept(ConceptId conceptId);

	List<DatasetCatalogRepository.StructureNode> listStructureNodes();

	Optional<DatasetCatalogRepository.StructureNode> findStructureNode(StructureNodeId structureNodeId);

	void saveStructureNode(DatasetCatalogRepository.StructureNode structureNode);

	boolean deleteStructureNode(StructureNodeId structureNodeId);

	List<DatasetCatalogRepository.TableRecord> listTables();

	Optional<DatasetCatalogRepository.TableRecord> findTable(TableId tableId);

	void saveTable(DatasetCatalogRepository.TableRecord table);

	boolean deleteTable(TableId tableId);
}
