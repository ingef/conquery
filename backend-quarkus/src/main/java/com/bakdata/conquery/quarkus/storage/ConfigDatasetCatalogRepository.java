package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Startup
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "IN_MEMORY", enableIfMissing = true)
public class ConfigDatasetCatalogRepository implements DatasetCatalogRepository {

	@Inject
	DatasetMetadataFolderLoader metadataFolderLoader;

	private Map<DatasetId, DatasetRecord> datasetsById = Map.of();
	private Map<DatasetId, Map<ConceptId, Concept>> conceptsByDatasetId = Map.of();
	private Map<DatasetId, Map<TableId, TableRecord>> tablesByDatasetId = Map.of();

	@PostConstruct
	void init() {
		Map<DatasetId, DatasetRecord> datasetIndex = new LinkedHashMap<>();
		Map<DatasetId, Map<ConceptId, Concept>> conceptDatasetIndex = new LinkedHashMap<>();
		Map<DatasetId, Map<TableId, TableRecord>> tableDatasetIndex = new LinkedHashMap<>();

		metadataFolderLoader.loadConfiguredDatasets().forEach(dataset -> {
			datasetIndex.put(dataset.dataset().id(), dataset.dataset());
			conceptDatasetIndex.put(dataset.dataset().id(), new LinkedHashMap<>(dataset.conceptsById()));
			tableDatasetIndex.put(dataset.dataset().id(), new LinkedHashMap<>(dataset.tablesById()));
		});

		datasetsById = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(datasetIndex));
		conceptsByDatasetId = freezeNestedMap(conceptDatasetIndex);
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
	public void saveDataset(DatasetRecord dataset) {
		throw readOnly();
	}

	@Override
	public boolean deleteDataset(DatasetId datasetId) {
		throw readOnly();
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
	public void saveConcept(Concept concept) {
		throw readOnly();
	}

	@Override
	public boolean deleteConcept(ConceptId conceptId) {
		throw readOnly();
	}

	public List<TableRecord> listTablesForDataset(DatasetId datasetId) {
		return tablesByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<TableRecord> findTable(TableId tableId) {
		return Optional.ofNullable(tablesByDatasetId.getOrDefault(tableId.datasetId(), Map.of()).get(tableId));
	}

	@Override
	public void saveTable(TableRecord table) {
		throw readOnly();
	}

	@Override
	public boolean deleteTable(TableId tableId) {
		throw readOnly();
	}

	private <K, T> Map<DatasetId, Map<K, T>> freezeNestedMap(Map<DatasetId, Map<K, T>> source) {
		Map<DatasetId, Map<K, T>> result = new LinkedHashMap<>();
		source.forEach((datasetId, entries) -> result.put(datasetId, java.util.Collections.unmodifiableMap(new LinkedHashMap<>(entries))));
		return java.util.Collections.unmodifiableMap(result);
	}

	private UnsupportedOperationException readOnly() {
		return new UnsupportedOperationException(
				"ConfigDatasetCatalogRepository is read-only. Adapt application configuration and restart."
		);
	}
}
