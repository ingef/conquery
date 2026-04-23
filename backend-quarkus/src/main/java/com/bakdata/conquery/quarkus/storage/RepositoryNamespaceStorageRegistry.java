package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.util.ScopedId;
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
	public Optional<NamespaceStorage> findNamespace(String datasetId) {
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
		public List<DatasetCatalogRepository.ConceptRecord> listConcepts() {
			return catalogRepository.listConceptsForDataset(dataset.id());
		}

		@Override
		public Optional<DatasetCatalogRepository.ConceptRecord> findConcept(String conceptId) {
			if (!belongsToNamespace(conceptId)) {
				return Optional.empty();
			}
			return catalogRepository.findConcept(conceptId);
		}

		@Override
		public void saveConcept(DatasetCatalogRepository.ConceptRecord concept) {
			if (!belongsToNamespace(concept.id())) {
				throw new IllegalArgumentException("Concept id does not belong to namespace '" + dataset.id() + "': " + concept.id());
			}
			catalogRepository.saveConcept(new DatasetCatalogRepository.ConceptRecord(concept.id(), concept.label()));
		}

		@Override
		public boolean deleteConcept(String conceptId) {
			if (!belongsToNamespace(conceptId)) {
				return false;
			}
			return catalogRepository.deleteConcept(conceptId);
		}

		@Override
		public List<DatasetCatalogRepository.TableRecord> listTables() {
			return catalogRepository.listTablesForDataset(dataset.id());
		}

		@Override
		public Optional<DatasetCatalogRepository.TableRecord> findTable(String tableId) {
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
		public boolean deleteTable(String tableId) {
			if (!belongsToNamespace(tableId)) {
				return false;
			}
			return catalogRepository.deleteTable(tableId);
		}

		private boolean belongsToNamespace(String scopedId) {
			return ScopedId.extractDatasetId(scopedId).map(dataset.id()::equals).orElse(false);
		}
	}
}
