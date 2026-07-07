package com.bakdata.conquery.quarkus.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
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
				new DatasetCatalogRepository.Concept(cid("demo.icd"), "ICD", null,  Map.of(

						cid("demo.icd.a00"), new DatasetCatalogRepository.ConceptElement(cid("demo.icd.a00"), "A00",null,cid("demo.icd"), List.of(cid("demo.icd.a00.a00_0")), equal("A00", "A000")),
						cid("demo.icd.a00.a00_0"), new DatasetCatalogRepository.ConceptElement(cid("demo.icd.a00.a00_0"), "A00.0",null, cid("demo.icd.a00"), List.of(), equal("A000"))

				), List.of(cid("demo.icd.a00")), null),
				new DatasetCatalogRepository.Concept(cid("demo.other"), "Other",null, null, null, List.of())
		));

		DatasetService.ConceptCodeResolution resolution = service.resolveConceptCodes("demo.icd", List.of("A00", "A000", "OTHER", "UNKNOWN"));

		assertEquals(List.of("demo.icd.a00", "demo.icd.a00.a00_0"), resolution.resolvedConcepts());
		assertEquals(List.of("OTHER", "UNKNOWN"), resolution.unknownCodes());
	}

	private DatasetCatalogRepository.ConceptCondition equal(String... values) {
		return new DatasetCatalogRepository.ConceptCondition("EQUAL", List.of(values), null, List.of());
	}

	private ConceptId cid(String value) {
		return ConceptId.parse(value);
	}

	private static DatasetId did(String value) {
		return DatasetId.parse(value);
	}

	private record TestNamespaceStorageRegistry(List<DatasetCatalogRepository.Concept> concepts) implements NamespaceStorageRegistry {
		@Override
		public List<DatasetCatalogRepository.DatasetRecord> listDatasets() {
			return List.of(new DatasetCatalogRepository.DatasetRecord(did("demo"), "Demo"));
		}

		@Override
		public Optional<NamespaceStorage> findNamespace(DatasetId datasetId) {
			if (!did("demo").equals(datasetId)) {
				return Optional.empty();
			}
			return Optional.of(new TestNamespaceStorage(concepts));
		}
	}

	private record TestNamespaceStorage(List<DatasetCatalogRepository.Concept> concepts) implements NamespaceStorage {
		@Override
		public DatasetCatalogRepository.DatasetRecord dataset() {
			return new DatasetCatalogRepository.DatasetRecord(did("demo"), "Demo");
		}

		@Override
		public List<DatasetCatalogRepository.Concept> listConcepts() {
			return concepts;
		}

		@Override
		public Optional<DatasetCatalogRepository.Concept> findConcept(ConceptId conceptId) {
			return concepts.stream().filter(concept -> concept.id().equals(conceptId)).findFirst();
		}

		@Override
		public void saveConcept(DatasetCatalogRepository.Concept concept) {
			throw unsupported();
		}

		@Override
		public boolean deleteConcept(ConceptId conceptId) {
			throw unsupported();
		}

		@Override
		public List<DatasetCatalogRepository.TableRecord> listTables() {
			return List.of();
		}

		@Override
		public Optional<DatasetCatalogRepository.TableRecord> findTable(TableId tableId) {
			return Optional.empty();
		}

		@Override
		public void saveTable(DatasetCatalogRepository.TableRecord table) {
			throw unsupported();
		}

		@Override
		public boolean deleteTable(TableId tableId) {
			throw unsupported();
		}

		private UnsupportedOperationException unsupported() {
			return new UnsupportedOperationException("Test storage is read-only.");
		}
	}
}
