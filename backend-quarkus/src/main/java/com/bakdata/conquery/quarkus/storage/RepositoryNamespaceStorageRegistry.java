package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.StructureNodeId;
import com.bakdata.conquery.quarkus.ids.TableId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RepositoryNamespaceStorageRegistry implements NamespaceStorageRegistry {

	@Inject
	DatasetCatalogRepository catalogRepository;

	@Override
	public List<DatasetCatalogRepository.DatasetRecord> listDatasets() {
		return catalogRepository.listDatasets();
	}

	@Override
	public Optional<NamespaceStorage> findNamespace(DatasetId datasetId) {
		return catalogRepository.findDataset(datasetId).map(dataset -> new RepositoryNamespaceStorage(dataset, catalogRepository));
	}

	private static final class RepositoryNamespaceStorage implements NamespaceStorage {
		private final DatasetCatalogRepository.DatasetRecord dataset;
		private final DatasetCatalogRepository catalogRepository;

		private RepositoryNamespaceStorage(
				DatasetCatalogRepository.DatasetRecord dataset,
				DatasetCatalogRepository catalogRepository
		) {
			this.dataset = dataset;
			this.catalogRepository = catalogRepository;
		}

		@Override
		public DatasetCatalogRepository.DatasetRecord dataset() {
			return dataset;
		}

		@Override
		public List<DatasetCatalogRepository.Concept> listConcepts() {
			return catalogRepository.listConceptsForDataset(dataset.id());
		}

		@Override
		public Optional<DatasetCatalogRepository.Concept> findConcept(ConceptId conceptId) {
			if (!belongsToNamespace(conceptId)) {
				return Optional.empty();
			}
			return catalogRepository.findConcept(conceptId);
		}

		@Override
		public void saveConcept(DatasetCatalogRepository.Concept concept) {
			if (!belongsToNamespace(concept.id())) {
				throw new IllegalArgumentException("Concept id does not belong to namespace '" + dataset.id() + "': " + concept.id());
			}
			catalogRepository.saveConcept(concept);
		}

		@Override
		public boolean deleteConcept(ConceptId conceptId) {
			if (!belongsToNamespace(conceptId)) {
				return false;
			}
			return catalogRepository.deleteConcept(conceptId);
		}

		@Override
		public List<DatasetCatalogRepository.StructureNode> listStructureNodes() {
			return catalogRepository.listStructureNodesForDataset(dataset.id());
		}

		@Override
		public Optional<DatasetCatalogRepository.StructureNode> findStructureNode(StructureNodeId structureNodeId) {
			if (!belongsToNamespace(structureNodeId)) {
				return Optional.empty();
			}
			return catalogRepository.findStructureNode(structureNodeId);
		}

		@Override
		public void saveStructureNode(DatasetCatalogRepository.StructureNode structureNode) {
			if (!belongsToNamespace(structureNode.id())) {
				throw new IllegalArgumentException("Structure node id does not belong to namespace '" + dataset.id() + "': " + structureNode.id());
			}
			catalogRepository.saveStructureNode(structureNode);
		}

		@Override
		public boolean deleteStructureNode(StructureNodeId structureNodeId) {
			if (!belongsToNamespace(structureNodeId)) {
				return false;
			}
			return catalogRepository.deleteStructureNode(structureNodeId);
		}

		@Override
		public List<DatasetCatalogRepository.TableRecord> listTables() {
			return catalogRepository.listTablesForDataset(dataset.id());
		}

		@Override
		public Optional<DatasetCatalogRepository.TableRecord> findTable(TableId tableId) {
			if (!belongsToNamespace(tableId)) {
				return Optional.empty();
			}
			return catalogRepository.findTable(tableId);
		}

		@Override
		public void saveTable(DatasetCatalogRepository.TableRecord table) {
			if (!belongsToNamespace(table.id())) {
				throw new IllegalArgumentException("Table id does not belong to namespace '" + dataset.id() + "': " + table.id());
			}
			catalogRepository.saveTable(new DatasetCatalogRepository.TableRecord(
					table.id(),
					table.label(),
					table.columns(),
					table.primaryColumn()
			));
		}

		@Override
		public boolean deleteTable(TableId tableId) {
			if (!belongsToNamespace(tableId)) {
				return false;
			}
			return catalogRepository.deleteTable(tableId);
		}

		private boolean belongsToNamespace(ConceptId scopedId) {
			return dataset.id().equals(scopedId.datasetId());
		}

		private boolean belongsToNamespace(TableId scopedId) {
			return dataset.id().equals(scopedId.datasetId());
		}

		private boolean belongsToNamespace(StructureNodeId scopedId) {
			return dataset.id().equals(scopedId.datasetId());
		}
	}
}
