package com.bakdata.conquery.quarkus.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.config.DatasetMetadataRuntimeConfig;
import com.bakdata.conquery.quarkus.util.ScopedId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatasetMetadataFolderLoader {

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
		DatasetPayload datasetPayload = loadDatasetPayload(folderPath).orElse(new DatasetPayload(null, null));
		String datasetId = firstNonBlank(datasetPayload.id(), folderName).orElse(folderName);
		String datasetLabel = firstNonBlank(datasetPayload.label(), folderName).orElse(folderName);
		DatasetCatalogRepository.DatasetRecord dataset = new DatasetCatalogRepository.DatasetRecord(datasetId, datasetLabel);

		Map<String, DatasetCatalogRepository.Concept> conceptsById = loadConcepts(folderPath, datasetId);
		Map<String, DatasetCatalogRepository.TableRecord> tablesById = loadTables(folderPath, datasetId);

		return new LoadedDatasetMetadata(
				dataset,
				Map.copyOf(conceptsById),
				Map.copyOf(tablesById)
		);
	}

	private Map<String, DatasetCatalogRepository.Concept> loadConcepts(Path folderPath, String datasetId) {
		Path conceptsDir = folderPath.resolve("conceptTrees");
		if (!Files.isDirectory(conceptsDir)) {
			throw new IllegalStateException("Metadata folder is missing conceptTrees/: " + folderPath);
		}

		Map<String, DatasetCatalogRepository.Concept> conceptsById = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(conceptsDir)) {
			files.filter(Files::isRegularFile)
				 .filter(path -> path.getFileName().toString().endsWith(".concept.json"))
				 .sorted()
				 .forEach(path -> {
					 ConceptPayload payload = read(path, ConceptPayload.class);
					 String conceptName = firstNonBlank(payload.name(), stripSuffix(path.getFileName().toString(), ".concept.json"))
							 .orElseThrow(() -> new IllegalStateException("Concept file has no name: " + path));
					 String conceptId = conceptName.equals(datasetId) ? conceptName : ensureDatasetScopedId(datasetId, conceptName);
					 conceptsById.put(conceptId,collectConcept(payload, conceptId));
				 });
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to list concept metadata in " + conceptsDir, e);
		}

		return conceptsById;
	}

	private DatasetCatalogRepository.Concept collectConcept(
			ConceptPayload payload,
			String thisId
			) {
		String conceptLabel = firstNonBlank(payload.label()).orElseGet(() -> payload.name);
		List<ConceptElementPayload> children = Optional.ofNullable(payload.children()).orElse(List.of());

		Map<String, DatasetCatalogRepository.ConceptElement> conceptElementsById = new LinkedHashMap<>();
		List<String> directChildIds = children.stream()
											  .map(child -> childId(thisId, child))
											  .toList();

		children.forEach(child -> collectConceptChildren(child, childId(thisId, child), thisId, conceptElementsById));

		List<DatasetCatalogRepository.Connector> connectors = Optional.ofNullable(payload.connectors()).orElse(List.of()).stream().map(p -> new DatasetCatalogRepository.Connector(
				p.column,
				p.label,
				p.name,
				Optional.ofNullable(p.selects()).orElse(List.of()),
				Optional.ofNullable(p.filters()).orElse(List.of()),
				Optional.ofNullable(p.validityDates()).orElse(List.of()),
				p.isDefault
		)).toList();


		return new DatasetCatalogRepository.Concept(thisId, conceptLabel, payload.description, Map.copyOf(conceptElementsById), directChildIds, connectors);
	}

	private void collectConceptChildren(
			ConceptElementPayload payload,
			String conceptId,
			String parentId,
			Map<String, DatasetCatalogRepository.ConceptElement> conceptElementsById
	) {
		String conceptLabel = firstNonBlank(payload.label()).orElse(payload.name);
		List<ConceptElementPayload> children = Optional.ofNullable(payload.children()).orElse(List.of());
		List<String> childIds = children.stream()
										.map(child -> childId(conceptId, child))
										.toList();
		DatasetCatalogRepository.ConceptCondition condition = toConceptCondition(payload.condition());

		conceptElementsById.put(
				conceptId,
				new DatasetCatalogRepository.ConceptElement(conceptId, conceptLabel, payload.description, parentId, childIds, condition)
		);
		children.forEach(child -> collectConceptChildren(child, childId(conceptId, child), conceptId, conceptElementsById));
	}


	private String childId(String parentId, ConceptElementPayload child) {
		String childName = firstNonBlank(child.name(), child.label())
				.orElseThrow(() -> new IllegalStateException("Concept child of " + parentId + " has no name."));
		return parentId + "." + childName;
	}

	private DatasetCatalogRepository.ConceptCondition toConceptCondition(ConditionPayload condition) {
		if (condition == null) {
			return null;
		}
		String type = firstNonBlank(condition.type())
				.orElseThrow(() -> new IllegalStateException("Concept condition is missing required type."));
		return new DatasetCatalogRepository.ConceptCondition(
				type,
				condition.values(),
				condition.column(),
				Optional.ofNullable(condition.conditions()).orElse(List.of()).stream()
						.map(this::toConceptCondition)
						.toList()
		);
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
		// TODO check if this handling is necessary and correct
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
			// TODO this looks like it's not necessarily including the table name
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

	private Optional<DatasetPayload> loadDatasetPayload(Path folderPath) {
		Path datasetFile = folderPath.resolve("dataset.json");
		if (!Files.isRegularFile(datasetFile)) {
			return Optional.empty();
		}
		return Optional.of(read(datasetFile, DatasetPayload.class));
	}

	private <T> T read(Path path, Class<T> type) {
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
			Map<String, DatasetCatalogRepository.Concept> conceptsById,
			Map<String, DatasetCatalogRepository.TableRecord> tablesById
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DatasetPayload(
			String id,
			String label
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConceptPayload(
			String name,
			String label,
			String description,
			List<ConceptElementPayload> children,
			List<ConnectorPayload> connectors
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConceptElementPayload(
			String name,
			String label,
			String description,
			List<ConceptElementPayload> children,
			ConditionPayload condition
	) {
	}

	private record ConnectorPayload(
			String column,
			String label,
			String name,
			List<DatasetCatalogRepository.Select> selects,
			List<DatasetCatalogRepository.Filter> filters,
			// Use internal rep directly as we won't need data mangling
			List<DatasetCatalogRepository.ValidityDate> validityDates,
			boolean isDefault
	)
	{
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	// TODO implement polymorphism
	private record SelectPayload(){}

	@JsonIgnoreProperties(ignoreUnknown = true)
	// TODO implement polymorphism
	private record FilterPayload(){}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConditionPayload(
			String type,
			List<String> values,
			String column,
			List<ConditionPayload> conditions
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

}
