package com.bakdata.conquery.quarkus.services;

import java.util.List;

import com.bakdata.conquery.quarkus.config.StorageRuntimeConfig;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.NamespaceStorage;
import com.bakdata.conquery.quarkus.storage.NamespaceStorageRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.resteasy.reactive.common.NotImplementedYet;

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
		return requireDataset(DatasetId.parse(datasetId));
	}

	public DatasetCatalogRepository.DatasetRecord requireDataset(DatasetId datasetId) {
		return requireNamespace(datasetId).dataset();
	}

	public DatasetCatalogRepository.Concept requireConcept(String conceptId) {
		return requireConcept(ConceptId.parse(conceptId));
	}

	public DatasetCatalogRepository.Concept requireConcept(ConceptId conceptId) {
		NamespaceStorage namespace = requireNamespace(conceptId.datasetId());
		return namespace.findConcept(conceptId).orElseThrow(() -> new NotFoundException("Unknown concept: " + conceptId));
	}

	public List<DatasetCatalogRepository.Concept> listRootConceptsForDataset(String datasetId) {
		return listRootConceptsForDataset(DatasetId.parse(datasetId));
	}

	public List<DatasetCatalogRepository.Concept> listRootConceptsForDataset(DatasetId datasetId) {
		return requireNamespace(datasetId).listConcepts();
	}

	public List<DatasetCatalogRepository.StructureNode> listStructureNodesForDataset(String datasetId) {
		return requireNamespace(DatasetId.parse(datasetId)).listStructureNodes();
	}

	public List<DatasetCatalogRepository.TableRecord> listTablesForDataset(String datasetId) {
		return listTablesForDataset(DatasetId.parse(datasetId));
	}

	public List<DatasetCatalogRepository.TableRecord> listTablesForDataset(DatasetId datasetId) {
		return requireNamespace(datasetId).listTables();
	}

	public DatasetCatalogRepository.TableRecord requireTable(String tableId) {
		return requireTable(TableId.parse(tableId));
	}

	public DatasetCatalogRepository.TableRecord requireTable(TableId tableId) {
		NamespaceStorage namespace = requireNamespace(tableId.datasetId());
		return namespace.findTable(tableId).orElseThrow(() -> new NotFoundException("Unknown table: " + tableId));
	}

	public ConceptCodeResolution resolveConceptCodes(String rootConceptId, List<String> codes) {
		ConceptId parsedRootConceptId = ConceptId.parse(rootConceptId);
		DatasetCatalogRepository.Concept rootConcept = requireConcept(parsedRootConceptId);
		NamespaceStorage namespace = requireNamespace(parsedRootConceptId.datasetId());

		// TODO implement with updated concept model
		throw new NotImplementedYet();

//		List<String> resolvedInOrder = codes.stream()
//										.map(this::normalizeCode)
//										.map(lookupByCode::get)
//										.filter(java.util.Objects::nonNull)
//										.toList();
//		List<String> unknownCodes = codes.stream()
//									 .filter(code -> !lookupByCode.containsKey(normalizeCode(code)))
//									 .toList();
//
//		return new ConceptCodeResolution(List.copyOf(new LinkedHashSet<>(resolvedInOrder)), unknownCodes);
	}

	private NamespaceStorage requireNamespace(DatasetId datasetId) {
		return namespaceStorageRegistry.findNamespace(datasetId)
									   .orElseThrow(() -> new NotFoundException("Unknown dataset: " + datasetId));
	}

	public record ConceptCodeResolution(
			List<String> resolvedConcepts,
			List<String> unknownCodes
	) {
	}
}
