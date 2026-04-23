package com.bakdata.conquery.quarkus.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.quarkus.api.config.StorageRuntimeConfig;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.NamespaceStorage;
import com.bakdata.conquery.quarkus.storage.NamespaceStorageRegistry;
import com.bakdata.conquery.quarkus.util.ScopedId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class DatasetService {

	@Inject
	NamespaceStorageRegistry namespaceStorageRegistry;

	@Inject
	@SuppressWarnings("unused")
	StorageRuntimeConfig storageRuntimeConfig;

	public List<DatasetCatalogRepository.DatasetRecord> listDatasets() {
		return namespaceStorageRegistry.listDatasets();
	}

	public DatasetCatalogRepository.DatasetRecord requireDataset(String datasetId) {
		return requireNamespace(datasetId).dataset();
	}

	public DatasetCatalogRepository.ConceptRecord requireConcept(String conceptId) {
		NamespaceStorage namespace = requireNamespaceForScopedObject(conceptId, "concept");
		return namespace.findConcept(conceptId).orElseThrow(() -> new NotFoundException("Unknown concept: " + conceptId));
	}

	public List<DatasetCatalogRepository.ConceptRecord> listConceptsForDataset(String datasetId) {
		return requireNamespace(datasetId).listConcepts();
	}

	public List<DatasetCatalogRepository.TableRecord> listTablesForDataset(String datasetId) {
		return requireNamespace(datasetId).listTables();
	}

	public DatasetCatalogRepository.TableRecord requireTable(String tableId) {
		NamespaceStorage namespace = requireNamespaceForScopedObject(tableId, "table");
		return namespace.findTable(tableId).orElseThrow(() -> new NotFoundException("Unknown table: " + tableId));
	}

	public ConceptCodeResolution resolveConceptCodes(String rootConceptId, List<String> codes) {
		requireConcept(rootConceptId);
		NamespaceStorage namespace = requireNamespaceForScopedObject(rootConceptId, "concept");

		// TODO This does not look right yet: Concept codes need to be resolved against the leaf-children of the provided root concept
		Map<String, String> lookupByCode = namespace.listConcepts().stream()
												.flatMap(concept -> java.util.stream.Stream.of(
														Map.entry(normalizeCode(concept.id()), concept.id()),
														Map.entry(normalizeCode(concept.label()), concept.id())
												))
												.collect(Collectors.toMap(
														Map.Entry::getKey,
														Map.Entry::getValue,
														(existing, ignored) -> existing
												));

		List<String> resolvedInOrder = codes.stream()
										.map(this::normalizeCode)
										.map(lookupByCode::get)
										.filter(java.util.Objects::nonNull)
										.toList();
		List<String> unknownCodes = codes.stream()
									 .filter(code -> !lookupByCode.containsKey(normalizeCode(code)))
									 .toList();

		return new ConceptCodeResolution(List.copyOf(new LinkedHashSet<>(resolvedInOrder)), unknownCodes);
	}

	private String normalizeCode(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private NamespaceStorage requireNamespace(String datasetId) {
		return namespaceStorageRegistry.findNamespace(datasetId)
									   .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	private NamespaceStorage requireNamespaceForScopedObject(String scopedId, String objectType) {
		return ScopedId.extractDatasetId(scopedId)
									   .flatMap(namespaceStorageRegistry::findNamespace)
									   .orElseThrow(() -> new NotFoundException("Unknown " + objectType + ": " + scopedId));
	}

	public record ConceptCodeResolution(
			List<String> resolvedConcepts,
			List<String> unknownCodes
	) {
	}
}
