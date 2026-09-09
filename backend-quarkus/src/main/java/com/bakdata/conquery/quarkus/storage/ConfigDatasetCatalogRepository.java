package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.StructureNodeId;
import com.bakdata.conquery.quarkus.ids.TableId;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
public class ConfigDatasetCatalogRepository implements DatasetCatalogRepository {

	@Inject
	DatasetMetadataFolderLoader metadataFolderLoader;

	private Map<DatasetId, DatasetRecord> datasetsById = Map.of();
	private Map<DatasetId, Map<ConceptId, Concept>> conceptsByDatasetId = Map.of();
	private Map<DatasetId, Map<StructureNodeId, StructureNode>> structureNodesByDatasetId = Map.of();
	private Map<DatasetId, Map<TableId, TableRecord>> tablesByDatasetId = Map.of();

	@PostConstruct
	void init() {
		Map<DatasetId, DatasetRecord> datasetIndex = new LinkedHashMap<>();
		Map<DatasetId, Map<ConceptId, Concept>> conceptDatasetIndex = new LinkedHashMap<>();
		Map<DatasetId, Map<StructureNodeId, StructureNode>> structureNodeDatasetIndex = new LinkedHashMap<>();
		Map<DatasetId, Map<TableId, TableRecord>> tableDatasetIndex = new LinkedHashMap<>();

		metadataFolderLoader.loadConfiguredDatasets().forEach(dataset -> {
			datasetIndex.put(dataset.dataset().id(), dataset.dataset());
			conceptDatasetIndex.put(dataset.dataset().id(), new LinkedHashMap<>(dataset.conceptsById()));
			structureNodeDatasetIndex.put(dataset.dataset().id(), new LinkedHashMap<>(dataset.structureNodesById()));
			tableDatasetIndex.put(dataset.dataset().id(), new LinkedHashMap<>(dataset.tablesById()));
		});

		datasetsById = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(datasetIndex));
		conceptsByDatasetId = freezeNestedMap(conceptDatasetIndex);
		structureNodesByDatasetId = freezeNestedMap(structureNodeDatasetIndex);
		tablesByDatasetId = freezeNestedMap(tableDatasetIndex);
	}

	@Override
	public List<DatasetRecord> listDatasets() {
		return datasetsById.values().stream().toList();
	}

	@Override
	public Optional<DatasetRecord> findDataset(DatasetId datasetId) {
		return Optional.ofNullable(datasetsById.get(datasetId));
	}

	@Override
	public List<Concept> listConceptsForDataset(DatasetId datasetId) {
		return conceptsByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<Concept> findConcept(ConceptId conceptId) {
		return Optional.ofNullable(conceptsByDatasetId.getOrDefault(conceptId.datasetId(), Map.of()).get(conceptId));
	}

	@Override
	public List<StructureNode> listStructureNodesForDataset(DatasetId datasetId) {
		return structureNodesByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<StructureNode> findStructureNode(StructureNodeId structureNodeId) {
		return Optional.ofNullable(structureNodesByDatasetId.getOrDefault(structureNodeId.datasetId(), Map.of()).get(structureNodeId));
	}

	public List<TableRecord> listTablesForDataset(DatasetId datasetId) {
		return tablesByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<TableRecord> findTable(TableId tableId) {
		return Optional.ofNullable(tablesByDatasetId.getOrDefault(tableId.datasetId(), Map.of()).get(tableId));
	}

	private <K, T> Map<DatasetId, Map<K, T>> freezeNestedMap(Map<DatasetId, Map<K, T>> source) {
		Map<DatasetId, Map<K, T>> result = new LinkedHashMap<>();
		source.forEach((datasetId, entries) -> result.put(datasetId, java.util.Collections.unmodifiableMap(new LinkedHashMap<>(entries))));
		return java.util.Collections.unmodifiableMap(result);
	}

}
