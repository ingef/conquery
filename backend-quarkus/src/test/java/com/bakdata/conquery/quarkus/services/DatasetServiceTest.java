package com.bakdata.conquery.quarkus.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.NamespaceStorage;
import com.bakdata.conquery.quarkus.storage.NamespaceStorageRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DatasetServiceTest {

	@Test
	@Disabled("Endpoint not yet implemented")
	void resolvesConceptCodesAgainstRequestedConceptSubtree() {
		DatasetService service = new DatasetService();
		service.namespaceStorageRegistry = new TestNamespaceStorageRegistry(List.of(
				new DatasetCatalogRepository.Concept("demo.icd", "ICD", null,  Map.of(

						"demo.icd.a00", new DatasetCatalogRepository.ConceptElement("demo.icd.a00", "A00",null,"demo.icd", List.of("demo.icd.a00.a00_0"), equal("A00", "A000")),
						"demo.icd.a00.a00_0", new DatasetCatalogRepository.ConceptElement("demo.icd.a00.a00_0", "A00.0",null, "demo.icd.a00", List.of(), equal("A000"))

				), List.of("demo.icd.a00"), null),
				new DatasetCatalogRepository.Concept("demo.other", "Other",null, null, null, List.of())
		));

		DatasetService.ConceptCodeResolution resolution = service.resolveConceptCodes("demo.icd", List.of("A00", "A000", "OTHER", "UNKNOWN"));

		assertEquals(List.of("demo.icd.a00", "demo.icd.a00.a00_0"), resolution.resolvedConcepts());
		assertEquals(List.of("OTHER", "UNKNOWN"), resolution.unknownCodes());
	}

	private DatasetCatalogRepository.ConceptCondition equal(String... values) {
		return new DatasetCatalogRepository.ConceptCondition("EQUAL", List.of(values), null, List.of());
	}

	private record TestNamespaceStorageRegistry(List<DatasetCatalogRepository.Concept> concepts) implements NamespaceStorageRegistry {
		@Override
		public List<DatasetCatalogRepository.DatasetRecord> listDatasets() {
			return List.of(new DatasetCatalogRepository.DatasetRecord("demo", "Demo"));
		}

		@Override
		public Optional<NamespaceStorage> findNamespace(String datasetId) {
			if (!"demo".equals(datasetId)) {
				return Optional.empty();
			}
			return Optional.of(new TestNamespaceStorage(concepts));
		}
	}

	private record TestNamespaceStorage(List<DatasetCatalogRepository.Concept> concepts) implements NamespaceStorage {
		@Override
		public DatasetCatalogRepository.DatasetRecord dataset() {
			return new DatasetCatalogRepository.DatasetRecord("demo", "Demo");
		}

		@Override
		public List<DatasetCatalogRepository.Concept> listConcepts() {
			return concepts;
		}

		@Override
		public Optional<DatasetCatalogRepository.Concept> findConcept(String conceptId) {
			return concepts.stream().filter(concept -> concept.id().equals(conceptId)).findFirst();
		}

		@Override
		public void saveConcept(DatasetCatalogRepository.Concept concept) {
			throw unsupported();
		}

		@Override
		public boolean deleteConcept(String conceptId) {
			throw unsupported();
		}

		@Override
		public List<DatasetCatalogRepository.TableRecord> listTables() {
			return List.of();
		}

		@Override
		public Optional<DatasetCatalogRepository.TableRecord> findTable(String tableId) {
			return Optional.empty();
		}

		@Override
		public void saveTable(DatasetCatalogRepository.TableRecord table) {
			throw unsupported();
		}

		@Override
		public boolean deleteTable(String tableId) {
			throw unsupported();
		}

		private UnsupportedOperationException unsupported() {
			return new UnsupportedOperationException("Test storage is read-only.");
		}
	}
}
