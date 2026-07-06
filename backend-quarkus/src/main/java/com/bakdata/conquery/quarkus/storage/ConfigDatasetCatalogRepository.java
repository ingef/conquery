package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.quarkus.util.ScopedId;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "IN_MEMORY", enableIfMissing = true)
public class ConfigDatasetCatalogRepository implements DatasetCatalogRepository {

	@Inject
	DatasetMetadataFolderLoader metadataFolderLoader;

	private Map<String, DatasetRecord> datasetsById = Map.of();
	private Map<String, Map<String, Concept>> conceptsByDatasetId = Map.of();
	private Map<String, Map<String, TableRecord>> tablesByDatasetId = Map.of();

	@PostConstruct
	void init() {
		Map<String, DatasetRecord> datasetIndex = new LinkedHashMap<>();
		Map<String, Map<String, Concept>> conceptDatasetIndex = new LinkedHashMap<>();
		Map<String, Map<String, TableRecord>> tableDatasetIndex = new LinkedHashMap<>();

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
	public Optional<DatasetRecord> findDataset(String datasetId) {
		return Optional.ofNullable(datasetsById.get(datasetId));
	}

	@Override
	public void saveDataset(DatasetRecord dataset) {
		throw readOnly();
	}

	@Override
	public boolean deleteDataset(String datasetId) {
		throw readOnly();
	}

	@Override
	public List<Concept> listConceptsForDataset(String datasetId) {
		return conceptsByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<Concept> findConcept(String conceptId) {
		return ScopedId.extractDatasetId(conceptId)
					   .flatMap(datasetId -> Optional.ofNullable(conceptsByDatasetId.getOrDefault(datasetId, Map.of()).get(conceptId)));
	}

	@Override
	public void saveConcept(Concept concept) {
		throw readOnly();
	}

	@Override
	public boolean deleteConcept(String conceptId) {
		throw readOnly();
	}

	public List<TableRecord> listTablesForDataset(String datasetId) {
		return tablesByDatasetId.getOrDefault(datasetId, Map.of()).values().stream().toList();
	}

	@Override
	public Optional<TableRecord> findTable(String tableId) {
		return ScopedId.extractDatasetId(tableId)
					   .flatMap(datasetId -> Optional.ofNullable(tablesByDatasetId.getOrDefault(datasetId, Map.of()).get(tableId)));
	}

	@Override
	public void saveTable(TableRecord table) {
		throw readOnly();
	}

	@Override
	public boolean deleteTable(String tableId) {
		throw readOnly();
	}

	private DatasetCatalogRepository.ColumnType parseColumnType(String type) {
		try {
			return DatasetCatalogRepository.ColumnType.valueOf(type.trim().toUpperCase(Locale.ROOT));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unsupported table column type: " + type, e);
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() || "__unset__".equals(value) ? null : value;
	}

	private void validateScopedIdBelongsToDataset(String scopedId, String datasetId, String objectType) {
		String derivedDataset = ScopedId.extractDatasetId(scopedId)
										.orElseThrow(() -> new IllegalArgumentException("Configured " + objectType + " id is blank"));
		if (!derivedDataset.equals(datasetId)) {
			throw new IllegalArgumentException(
					"Configured " + objectType + " id '" + scopedId + "' does not match dataset '" + datasetId + "'."
			);
		}
	}

	private <T> Map<String, Map<String, T>> freezeNestedMap(Map<String, Map<String, T>> source) {
		Map<String, Map<String, T>> result = new LinkedHashMap<>();
		source.forEach((datasetId, entries) -> result.put(datasetId, java.util.Collections.unmodifiableMap(new LinkedHashMap<>(entries))));
		return java.util.Collections.unmodifiableMap(result);
	}

	private UnsupportedOperationException readOnly() {
		return new UnsupportedOperationException(
				"ConfigDatasetCatalogRepository is read-only. Adapt application configuration and restart."
		);
	}
}
