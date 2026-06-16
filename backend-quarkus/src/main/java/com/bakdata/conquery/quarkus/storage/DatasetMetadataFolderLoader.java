package com.bakdata.conquery.quarkus.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.api.config.DatasetMetadataRuntimeConfig;
import com.bakdata.conquery.quarkus.util.ScopedId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatasetMetadataFolderLoader {

	private static final TypeReference<List<StructureEntryPayload>> STRUCTURE_PAYLOAD_LIST = new TypeReference<>() {
	};

	@Inject
	DatasetMetadataRuntimeConfig metadataConfig;

	@Inject
	ObjectMapper objectMapper;

	public List<LoadedDatasetMetadata> loadConfiguredDatasets() {
		if (!metadataConfig.enabled()) {
			return List.of();
		}

		String rootPath = metadataConfig.rootPath()
										 .orElseThrow(() -> new IllegalStateException("conquery.metadata.root-path is required when metadata ingestion is enabled."));
		List<String> folders = metadataConfig.folders()
											 .orElseThrow(() -> new IllegalStateException("conquery.metadata.folders must contain at least one folder when metadata ingestion is enabled."));
		if (folders.isEmpty()) {
			throw new IllegalStateException("conquery.metadata.folders must not be empty when metadata ingestion is enabled.");
		}

		Map<String, LoadedDatasetMetadata> loadedByDatasetId = new LinkedHashMap<>();
		Path root = Path.of(rootPath);
		for (String configuredFolder : folders) {
			Path folderPath = toFolderPath(root, configuredFolder);
			LoadedDatasetMetadata loaded = loadDatasetFolder(folderPath);
			LoadedDatasetMetadata previous = loadedByDatasetId.put(loaded.dataset().id(), loaded);
			if (previous != null) {
				throw new IllegalStateException("Duplicate dataset id '" + loaded.dataset().id() + "' across configured metadata folders.");
			}
		}

		return loadedByDatasetId.values().stream().toList();
	}

	private LoadedDatasetMetadata loadDatasetFolder(Path folderPath) {
		if (!Files.isDirectory(folderPath)) {
			throw new IllegalStateException("Configured metadata folder does not exist or is not a directory: " + folderPath);
		}

		String folderName = folderPath.getFileName() == null ? folderPath.toString() : folderPath.getFileName().toString();
		String datasetId = resolveDatasetId(folderPath).orElse(folderName);
		DatasetCatalogRepository.DatasetRecord dataset = new DatasetCatalogRepository.DatasetRecord(datasetId, folderName);

		Map<String, DatasetCatalogRepository.ConceptRecord> conceptsById = loadConcepts(folderPath, datasetId);
		Map<String, DatasetCatalogRepository.TableRecord> tablesById = loadTables(folderPath, datasetId);

		return new LoadedDatasetMetadata(
				dataset,
				Map.copyOf(conceptsById),
				Map.copyOf(tablesById)
		);
	}

	private Map<String, DatasetCatalogRepository.ConceptRecord> loadConcepts(Path folderPath, String datasetId) {
		Path conceptsDir = folderPath.resolve("conceptTrees");
		if (!Files.isDirectory(conceptsDir)) {
			throw new IllegalStateException("Metadata folder is missing conceptTrees/: " + folderPath);
		}

		Map<String, DatasetCatalogRepository.ConceptRecord> conceptsById = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(conceptsDir)) {
			files.filter(Files::isRegularFile)
				 .filter(path -> path.getFileName().toString().endsWith(".concept.json"))
				 .sorted()
				 .forEach(path -> {
					 ConceptPayload payload = read(path, ConceptPayload.class);
					 String conceptName = firstNonBlank(payload.name(), stripSuffix(path.getFileName().toString(), ".concept.json"))
							 .orElseThrow(() -> new IllegalStateException("Concept file has no name: " + path));
					 String conceptId = ensureDatasetScopedId(datasetId, conceptName);
					 String conceptLabel = firstNonBlank(payload.label(), conceptName).orElse(conceptName);
					 conceptsById.put(conceptId, new DatasetCatalogRepository.ConceptRecord(conceptId, conceptLabel));
				 });
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to list concept metadata in " + conceptsDir, e);
		}

		return conceptsById;
	}

	private Map<String, DatasetCatalogRepository.TableRecord> loadTables(Path folderPath, String datasetId) {
		Path tablesDir = folderPath.resolve("tables");
		if (!Files.isDirectory(tablesDir)) {
			throw new IllegalStateException("Metadata folder is missing tables/: " + folderPath);
		}

		Map<String, DatasetCatalogRepository.TableRecord> tablesById = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(tablesDir)) {
			files.filter(Files::isRegularFile)
				 .filter(path -> path.getFileName().toString().endsWith(".table.json"))
				 .sorted()
				 .forEach(path -> {
					 TablePayload payload = read(path, TablePayload.class);
					 String tableName = firstNonBlank(payload.name(), stripSuffix(path.getFileName().toString(), ".table.json"))
							 .orElseThrow(() -> new IllegalStateException("Table file has no name: " + path));
					 String tableId = ensureDatasetScopedId(datasetId, tableName);
					 String tableLabel = firstNonBlank(payload.label(), tableName).orElse(tableName);
					 List<DatasetCatalogRepository.ColumnRecord> columns = Optional.ofNullable(payload.columns()).orElse(List.of()).stream()
																					.map(column -> toColumn(datasetId, tableName, tableId, column))
																					.toList();
					 String primaryColumn = toColumnId(datasetId, tableName, tableId, payload.primaryColumn()).orElse(null);
					 tablesById.put(tableId, new DatasetCatalogRepository.TableRecord(tableId, tableLabel, columns, primaryColumn));
				 });
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to list table metadata in " + tablesDir, e);
		}

		return tablesById;
	}

	private DatasetCatalogRepository.ColumnRecord toColumn(
			String datasetId,
			String tableName,
			String tableId,
			ColumnPayload payload
	) {
		String rawColumnName = firstNonBlank(payload.name(), payload.id())
				.orElseThrow(() -> new IllegalStateException("Column entry is missing a name/id in table " + tableId));
		String columnId = toColumnId(datasetId, tableName, tableId, rawColumnName)
				.orElseThrow(() -> new IllegalStateException("Column id must not be blank in table " + tableId));
		String columnLabel = firstNonBlank(payload.label(), rawColumnName).orElse(rawColumnName);
		DatasetCatalogRepository.ColumnType type = parseColumnType(payload.type());
		String secondaryId = normalizeBlank(payload.secondaryId()).orElse(null);
		return new DatasetCatalogRepository.ColumnRecord(columnId, columnLabel, type, secondaryId);
	}

	private Optional<String> toColumnId(String datasetId, String tableName, String tableId, String rawId) {
		Optional<String> value = normalizeBlank(rawId);
		if (value.isEmpty()) {
			return Optional.empty();
		}
		String id = value.get();
		if (id.startsWith(tableId + ".")) {
			return Optional.of(id);
		}
		if (id.startsWith(tableName + ".")) {
			return Optional.of(datasetId + "." + id);
		}
		if (ScopedId.extractDatasetId(id).filter(datasetId::equals).isPresent() && id.contains(".")) {
			return Optional.of(id);
		}
		if (id.contains(".")) {
			return Optional.of(datasetId + "." + id);
		}
		return Optional.of(tableId + "." + id);
	}

	private String ensureDatasetScopedId(String datasetId, String rawId) {
		String id = normalizeBlank(rawId).orElseThrow(() -> new IllegalStateException("Id must not be blank."));
		if (ScopedId.extractDatasetId(id).filter(datasetId::equals).isPresent() && id.contains(".")) {
			return id;
		}
		return datasetId + "." + id;
	}

	private Optional<String> resolveDatasetId(Path folderPath) {
		List<Path> structureFiles;
		try (Stream<Path> files = Files.list(folderPath)) {
			structureFiles = files.filter(Files::isRegularFile)
								  .filter(path -> path.getFileName().toString().startsWith("structure"))
								  .filter(path -> path.getFileName().toString().endsWith(".json"))
								  .sorted()
								  .toList();
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to list structure files in metadata folder " + folderPath, e);
		}

		Set<String> datasetIds = structureFiles.stream()
											   .flatMap(path -> readStructureDatasetIds(path).stream())
											   .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
		if (datasetIds.isEmpty()) {
			return Optional.empty();
		}
		if (datasetIds.size() > 1) {
			throw new IllegalStateException("Metadata folder contains multiple dataset ids in structure files: " + folderPath + " -> " + datasetIds);
		}
		return datasetIds.stream().findFirst();
	}

	private List<String> readStructureDatasetIds(Path structureFile) {
		List<StructureEntryPayload> payload = read(structureFile, STRUCTURE_PAYLOAD_LIST);
		return payload.stream()
					  .map(StructureEntryPayload::datasetValue)
					  .flatMap(Optional::stream)
					  .toList();
	}

	private <T> T read(Path path, Class<T> type) {
		try {
			return objectMapper.readValue(path.toFile(), type);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to parse metadata file: " + path, e);
		}
	}

	private <T> T read(Path path, TypeReference<T> type) {
		try {
			return objectMapper.readValue(path.toFile(), type);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to parse metadata file: " + path, e);
		}
	}

	private Path toFolderPath(Path root, String configuredFolder) {
		String value = normalizeBlank(configuredFolder)
				.orElseThrow(() -> new IllegalStateException("conquery.metadata.folders must not contain blank entries."));
		Path configuredPath = Path.of(value);
		return configuredPath.isAbsolute() ? configuredPath : root.resolve(configuredPath);
	}

	private DatasetCatalogRepository.ColumnType parseColumnType(String rawType) {
		String type = normalizeBlank(rawType)
				.orElseThrow(() -> new IllegalStateException("Table column type must not be blank."));
		try {
			return DatasetCatalogRepository.ColumnType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
		}
		catch (Exception e) {
			throw new IllegalStateException("Unsupported table column type: " + rawType, e);
		}
	}

	private Optional<String> firstNonBlank(String... candidates) {
		for (String candidate : candidates) {
			Optional<String> value = normalizeBlank(candidate);
			if (value.isPresent()) {
				return value;
			}
		}
		return Optional.empty();
	}

	private Optional<String> normalizeBlank(String value) {
		if (value == null) {
			return Optional.empty();
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}

	private String stripSuffix(String fileName, String suffix) {
		if (fileName.endsWith(suffix)) {
			return fileName.substring(0, fileName.length() - suffix.length());
		}
		return fileName;
	}

	public record LoadedDatasetMetadata(
			DatasetCatalogRepository.DatasetRecord dataset,
			Map<String, DatasetCatalogRepository.ConceptRecord> conceptsById,
			Map<String, DatasetCatalogRepository.TableRecord> tablesById
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record StructureEntryPayload(
			String dataset
	) {
		Optional<String> datasetValue() {
			return normalize(dataset);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConceptPayload(
			String name,
			String label
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TablePayload(
			String name,
			String label,
			String primaryColumn,
			List<ColumnPayload> columns
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ColumnPayload(
			String id,
			String name,
			String label,
			String type,
			String secondaryId
	) {
	}

	private static Optional<String> normalize(String value) {
		if (value == null) {
			return Optional.empty();
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}
}
