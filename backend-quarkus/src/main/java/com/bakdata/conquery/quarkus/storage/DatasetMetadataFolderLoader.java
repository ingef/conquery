package com.bakdata.conquery.quarkus.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import com.bakdata.conquery.quarkus.config.DatasetMetadataRuntimeConfig;
import com.bakdata.conquery.quarkus.concepts.filters.FilterConversionContext;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionProvider;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionRegistry;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.IdPartSanitizer;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.*;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class DatasetMetadataFolderLoader {

	@Inject
	DatasetMetadataRuntimeConfig metadataConfig;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	Validator validator;

	@Inject
	FilterDefinitionRegistry filterDefinitionRegistry;

	public List<LoadedDatasetMetadata> loadConfiguredDatasets() {
		if (!metadataConfig.enabled()) {
			return List.of();
		}

		String rootPath = metadataConfig.rootPath().orElseThrow(() -> new IllegalStateException("conquery.metadata.root-path is required when metadata ingestion is enabled."));
		List<String> folders = metadataConfig.folders().orElseThrow(() -> new IllegalStateException("conquery.metadata.folders must contain at least one folder when metadata ingestion is enabled."));
		if (folders.isEmpty()) {
			throw new IllegalStateException("conquery.metadata.folders must not be empty when metadata ingestion is enabled.");
		}

		Map<DatasetId, LoadedDatasetMetadata> loadedByDatasetId = new LinkedHashMap<>();
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
		DatasetId datasetId = DatasetId.parse(idPartFromPreferredOrFallback(datasetPayload.id(), folderName, "dataset id", folderPath));
		String datasetLabel = firstNonBlank(datasetPayload.label(), folderName).orElse(folderName);
		DatasetCatalogRepository.DatasetRecord dataset = new DatasetCatalogRepository.DatasetRecord(datasetId, datasetLabel);

		Map<TableId, DatasetCatalogRepository.TableRecord> tablesById = loadTables(folderPath, datasetId);
		Map<ConceptId, DatasetCatalogRepository.Concept> conceptsById = loadConcepts(folderPath, datasetId, tablesById);

		log.debug("Loaded dataset {} with {} concepts and {} tables", datasetId, conceptsById.size(), tablesById.size());
		return new LoadedDatasetMetadata(dataset, Map.copyOf(conceptsById), Map.copyOf(tablesById));
	}

	private Map<ConceptId, DatasetCatalogRepository.Concept> loadConcepts(Path folderPath, DatasetId datasetId, Map<TableId, DatasetCatalogRepository.TableRecord> tablesById) {
		Path conceptsDir = folderPath.resolve("conceptTrees");
		if (!Files.isDirectory(conceptsDir)) {
			throw new IllegalStateException("Metadata folder is missing conceptTrees/: " + folderPath);
		}

		Map<ConceptId, DatasetCatalogRepository.Concept> conceptsById = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(conceptsDir)) {
			files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".concept.json")).sorted().forEach(path -> {
				ConceptPayload payload = read(path, ConceptPayload.class);
				String conceptName = idPartFromPreferredOrFallback(payload.name(), stripSuffix(path.getFileName().toString(), ".concept.json"), "concept id", path);

				Set<ConstraintViolation<ConceptPayload>> constraintViolations = validator.validate(payload);
				if (!constraintViolations.isEmpty()) {
					throw new ConstraintViolationException("Failed to validate concept payload: %s".formatted(path), constraintViolations);
				}
				ConceptId conceptId = conceptName.equals(datasetId.name()) ? new ConceptId(datasetId, List.of()) : new ConceptId(datasetId, List.of(conceptName));
				conceptsById.put(conceptId, collectConcept(payload, conceptId, tablesById));
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to list concept metadata in " + conceptsDir, e);
		}

		log.debug("Loaded dataset {} with {} concepts", datasetId, conceptsById.size());
		return conceptsById;
	}

	private DatasetCatalogRepository.Concept collectConcept(ConceptPayload payload, ConceptId conceptId, Map<TableId, DatasetCatalogRepository.TableRecord> tablesById) {
		String conceptLabel = firstNonBlank(payload.label()).orElseGet(() -> payload.name);
		List<ConceptElementPayload> children = Optional.ofNullable(payload.children()).orElse(List.of());
		FallbackLogCollector fallbackLogCollector = new FallbackLogCollector(conceptId);

		Map<ConceptId, DatasetCatalogRepository.ConceptElement> conceptElementsById = new LinkedHashMap<>();
		List<ConceptId> directChildIds = children.stream().map(child -> childId(conceptId, child, fallbackLogCollector)).toList();

		for (int index = 0; index < children.size(); index++) {
			collectConceptChildren(children.get(index), directChildIds.get(index), conceptId, conceptElementsById, fallbackLogCollector);
		}

		List<DatasetCatalogRepository.Connector> connectors = payload.connectors().stream().map(p -> {

			DatasetId datasetId = conceptId.datasetId();
			String tableName = normalizeBlank(p.table)
					.orElseThrow(() -> new IllegalStateException("Connector '" + p.name + "' in concept '" + conceptId + "' must define a table."));
			TableId tableId = toConnectorTableId(datasetId, tableName);
			DatasetCatalogRepository.TableRecord table = Optional.ofNullable(tablesById.get(tableId))
					.orElseThrow(() -> new IllegalStateException("Connector '" + p.name + "' in concept '" + conceptId + "' references unknown table '" + tableId + "'."));
			String columnName = p.column;
			if (columnName != null) {
				columnName = normalizeLocalColumnName(columnName);
			}

			ConnectorId connectorId = new ConnectorId(conceptId, p.name);
			return new DatasetCatalogRepository.Connector(
					connectorId,
					tableId,
					columnName == null ? null : new ColumnId(tableId, columnName),
					p.label,
					p.name,
					List.of(), // Optional.of(p.selects().stream().map(SelectPayload::new).toList()).orElse(List.of()),
					convertFilters(connectorId, tableId, table, p.filters(), fallbackLogCollector),
					Optional.ofNullable(p.validityDates()).orElse(List.of()),
					p.isDefault
			);

		}).toList();

		log.debug("Loaded concept {} with {} children", conceptId, children.size());
		fallbackLogCollector.logSummary();

		return new DatasetCatalogRepository.Concept(conceptId, conceptLabel, payload.description, Map.copyOf(conceptElementsById), directChildIds, connectors);
	}

	private void collectConceptChildren(ConceptElementPayload payload, ConceptId conceptId, ConceptId parentId, Map<ConceptId, DatasetCatalogRepository.ConceptElement> conceptElementsById, FallbackLogCollector fallbackLogCollector) {
		String conceptLabel = firstNonBlank(payload.label()).orElse(payload.name);
		List<ConceptElementPayload> children = Optional.ofNullable(payload.children()).orElse(List.of());
		List<ConceptId> childIds = children.stream().map(child -> childId(conceptId, child, fallbackLogCollector)).toList();
		DatasetCatalogRepository.ConceptCondition condition = toConceptCondition(payload.condition());

		conceptElementsById.put(conceptId, new DatasetCatalogRepository.ConceptElement(conceptId, conceptLabel, payload.description, parentId, childIds, condition));
		for (int index = 0; index < children.size(); index++) {
			collectConceptChildren(children.get(index), childIds.get(index), conceptId, conceptElementsById, fallbackLogCollector);
		}
	}


	private ConceptId childId(ConceptId parentId, ConceptElementPayload child, FallbackLogCollector fallbackLogCollector) {
		String childName = idPartFromPreferredOrFallback(child.name(), child.label(), "concept child id", parentId, fallbackLogCollector);
		return parentId.child(childName);
	}

	private DatasetCatalogRepository.ConceptCondition toConceptCondition(ConditionPayload condition) {
		if (condition == null) {
			return null;
		}
		String type = firstNonBlank(condition.type()).orElseThrow(() -> new IllegalStateException("Concept condition is missing required type."));
		return new DatasetCatalogRepository.ConceptCondition(type, condition.values(), condition.column(), Optional.ofNullable(condition.conditions()).orElse(List.of()).stream().map(this::toConceptCondition).toList());
	}

	private TableId toConnectorTableId(DatasetId datasetId, String rawTableName) {
		String tableName = rawTableName;
		if (tableName.startsWith(datasetId + ".")) {
			tableName = tableName.substring(datasetId.toString().length() + 1);
		}
		return new TableId(datasetId, tableName);
	}

	private List<DatasetCatalogRepository.Filter> convertFilters(ConnectorId connectorId, TableId tableId, DatasetCatalogRepository.TableRecord table, JsonNode filters, FallbackLogCollector fallbackLogCollector) {
		if (filters == null || filters.isNull()) {
			return List.of();
		}
		List<JsonNode> rawFilters;
		if (filters.isArray()) {
			rawFilters = new ArrayList<>();
			filters.forEach(rawFilters::add);
		}
		else if (filters.isObject()) {
			rawFilters = List.of(filters);
		}
		else {
			throw new IllegalStateException("Connector '" + connectorId + "' filters must be an object or array.");
		}

		FilterConversionContext context = new FilterConversionContext(connectorId, tableId, table, fallbackLogCollector::add);
		List<DatasetCatalogRepository.Filter> converted = new ArrayList<>();
		for (JsonNode rawFilter : rawFilters) {
			Optional<DatasetCatalogRepository.Filter> filter = convertFilter(context, rawFilter);
			filter.ifPresent(converted::add);
		}
		return List.copyOf(converted);
	}

	private Optional<DatasetCatalogRepository.Filter> convertFilter(FilterConversionContext context, JsonNode rawFilter) {
		String type = normalizeBlank(rawFilter.path("type").asText(null)).orElse(null);
		if (type == null) {
			return unknownFilter(context, rawFilter, "missing filter type");
		}
		Optional<FilterDefinitionProvider<?>> provider = filterDefinitionRegistry.find(type);
		return provider
				.map(filterDefinitionProvider -> convertFilterWithProvider(context, rawFilter, filterDefinitionProvider))
				.or(() -> unknownFilter(context, rawFilter, "unknown filter type '" + type + "'"));
	}

	private Optional<DatasetCatalogRepository.Filter> unknownFilter(FilterConversionContext context, JsonNode rawFilter, String reason) {
		String message = "Skipping filter for connector '" + context.connectorId() + "' because of " + reason + ": " + rawFilter;
		if (metadataConfig.strictFilterTypes()) {
			throw new IllegalStateException(message);
		}
		log.warn("{}", message);
		return Optional.empty();
	}

	private <T> DatasetCatalogRepository.Filter convertFilterWithProvider(FilterConversionContext context, JsonNode rawFilter, FilterDefinitionProvider<T> provider) {
		T payload;
		try {
			payload = objectMapper.treeToValue(rawFilter, provider.payloadType());
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to parse filter of type '" + provider.type() + "' for connector '" + context.connectorId() + "'.", e);
		}
		Set<ConstraintViolation<T>> constraintViolations = validator.validate(payload);
		if (!constraintViolations.isEmpty()) {
			throw new ConstraintViolationException("Failed to validate filter payload for connector '%s'.".formatted(context.connectorId()), constraintViolations);
		}
		return provider.convert(context, payload);
	}

	private Map<TableId, DatasetCatalogRepository.TableRecord> loadTables(Path folderPath, DatasetId datasetId) {
		Path tablesDir = folderPath.resolve("tables");
		if (!Files.isDirectory(tablesDir)) {
			throw new IllegalStateException("Metadata folder is missing tables/: " + folderPath);
		}

		Map<TableId, DatasetCatalogRepository.TableRecord> tablesById = new LinkedHashMap<>();
		try (Stream<Path> files = Files.list(tablesDir)) {
			files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".table.json")).sorted().forEach(path -> {
				TablePayload payload = read(path, TablePayload.class);
				String tableName = idPartFromPreferredOrFallback(payload.name(), stripSuffix(path.getFileName().toString(), ".table.json"), "table id", path);
				TableId tableId = new TableId(datasetId, tableName);
				String tableLabel = firstNonBlank(payload.label(), tableName).orElse(tableName);
				List<DatasetCatalogRepository.ColumnRecord> columns = Optional.ofNullable(payload.columns()).orElse(List.of()).stream().map(column -> toColumn(datasetId, tableName, tableId, column)).toList();
				ColumnId primaryColumn = toColumnId(datasetId, tableName, tableId, payload.primaryColumn()).orElse(null);
				tablesById.put(tableId, new DatasetCatalogRepository.TableRecord(tableId, tableLabel, columns, primaryColumn));
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to list table metadata in " + tablesDir, e);
		}

		return tablesById;
	}

	private DatasetCatalogRepository.ColumnRecord toColumn(DatasetId datasetId, String tableName, TableId tableId, ColumnPayload payload) {
		String rawColumnName = firstNonBlank(payload.name(), payload.id()).orElseThrow(() -> new IllegalStateException("Column entry is missing a name/id in table " + tableId));
		ColumnId columnId = toColumnId(datasetId, tableName, tableId, rawColumnName).orElseThrow(() -> new IllegalStateException("Column id must not be blank in table " + tableId));
		String columnLabel = firstNonBlank(payload.label(), rawColumnName).orElse(rawColumnName);
		DatasetCatalogRepository.ColumnType type = parseColumnType(payload.type());
		String secondaryId = normalizeBlank(payload.secondaryId()).orElse(null);
		return new DatasetCatalogRepository.ColumnRecord(columnId, columnLabel, type, secondaryId);
	}

	private Optional<ColumnId> toColumnId(DatasetId datasetId, String tableName, TableId tableId, String rawId) {
		Optional<String> value = normalizeBlank(rawId);
		// TODO check if this handling is necessary and correct
		if (value.isEmpty()) {
			return Optional.empty();
		}
		String id = value.get();
		if (id.startsWith(tableId + ".")) {
			return Optional.of(ColumnId.parse(id));
		}
		if (id.startsWith(tableName + ".")) {
			return Optional.of(ColumnId.parse(datasetId + "." + id));
		}
		if (id.startsWith(datasetId + ".") && id.contains(".")) {
			// TODO this looks like it's not necessarily including the table name
			return Optional.of(ColumnId.parse(id));
		}
		if (id.contains(".")) {
			return Optional.of(ColumnId.parse(datasetId + "." + id));
		}
		return Optional.of(new ColumnId(tableId, id));
	}

	private String normalizeLocalColumnName(String rawColumnName) {
		String columnName = normalizeBlank(rawColumnName).orElseThrow(() -> new IllegalStateException("Connector column" + " must not be blank."));
		if (columnName.contains(".")) {
			throw new IllegalStateException("Connector column must reference a local column name without dots: " + rawColumnName);
		}
		return columnName;
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
		} catch (IOException e) {
			throw new IllegalStateException("Failed to parse metadata file: " + path, e);
		}
	}

	private Path toFolderPath(Path root, String configuredFolder) {
		String value = normalizeBlank(configuredFolder).orElseThrow(() -> new IllegalStateException("conquery.metadata.folders must not contain blank entries."));
		Path configuredPath = Path.of(value);
		return configuredPath.isAbsolute() ? configuredPath : root.resolve(configuredPath);
	}

	private DatasetCatalogRepository.ColumnType parseColumnType(String rawType) {
		String type = normalizeBlank(rawType).orElseThrow(() -> new IllegalStateException("Table column type must not be blank."));
		try {
			return DatasetCatalogRepository.ColumnType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
		} catch (Exception e) {
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

	private String idPartFromPreferredOrFallback(String preferred, String fallback, String idType, Object context) {
		return idPartFromPreferredOrFallback(preferred, fallback, idType, context, null);
	}

	private String idPartFromPreferredOrFallback(String preferred, String fallback, String idType, Object context, FallbackLogCollector fallbackLogCollector) {
		Optional<String> preferredValue = normalizeBlank(preferred);
		if (preferredValue.isPresent()) {
			return preferredValue.get();
		}
		String fallbackValue = normalizeBlank(fallback)
				.orElseThrow(() -> new IllegalStateException("Cannot derive " + idType + " for " + context + " because preferred and fallback values are blank."));
		String sanitized = IdPartSanitizer.sanitize(fallbackValue, idType + " fallback");
		if (fallbackLogCollector != null) {
			fallbackLogCollector.add(idType, context, fallbackValue, sanitized);
			return sanitized;
		}
		if (sanitized.equals(fallbackValue)) {
			log.info("Using {} fallback '{}' for {}", idType, sanitized, context);
		}
		else {
			log.info("Using sanitized {} fallback '{}' -> '{}' for {}", idType, fallbackValue, sanitized, context);
		}
		return sanitized;
	}

	private record FallbackLogEntry(
			String idType,
			Object context,
			String fallbackValue,
			String sanitized
	) {
		String message() {
			if (sanitized.equals(fallbackValue)) {
				return "Using " + idType + " fallback '" + sanitized + "' for " + context;
			}
			return "Using sanitized " + idType + " fallback '" + fallbackValue + "' -> '" + sanitized + "' for " + context;
		}
	}

	private static final class FallbackLogCollector {
		private static final int INFO_LIMIT = 5;

		private final ConceptId conceptId;
		private final List<FallbackLogEntry> entries = new ArrayList<>();

		private FallbackLogCollector(ConceptId conceptId) {
			this.conceptId = conceptId;
		}

		private void add(String idType, Object context, String fallbackValue, String sanitized) {
			entries.add(new FallbackLogEntry(idType, context, fallbackValue, sanitized));
		}

		private void logSummary() {
			if (entries.isEmpty()) {
				return;
			}
			if (log.isTraceEnabled()) {
				entries.forEach(entry -> log.trace("{}", entry.message()));
				return;
			}
			entries.stream()
				   .limit(INFO_LIMIT)
				   .forEach(entry -> log.info("{}", entry.message()));
			int remaining = entries.size() - INFO_LIMIT;
			if (remaining > 0) {
				log.info("Suppressed {} additional id fallback messages for concept {}. Enable trace logging to show all.", remaining, conceptId);
			}
		}
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
			DatasetCatalogRepository.DatasetRecord dataset, Map<ConceptId, DatasetCatalogRepository.Concept> conceptsById,
			Map<TableId, DatasetCatalogRepository.TableRecord> tablesById
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DatasetPayload(
			String id, String label
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConceptPayload(
			String name, String label, String description, List<ConceptElementPayload> children,
			@NotNull List<ConnectorPayload> connectors
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConceptElementPayload(
			String name, String label, String description, List<ConceptElementPayload> children,
			ConditionPayload condition
	) {
	}

	private record ConnectorPayload(
			String table, String column, String label, String name, List<SelectPayload> selects,
			JsonNode filters,
			// Use internal rep directly as we won't need data mangling
			List<DatasetCatalogRepository.ValidityDate> validityDates, boolean isDefault
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	// TODO implement polymorphism
	private record SelectPayload() implements DatasetCatalogRepository.Select {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConditionPayload(
			String type, List<String> values, String column, List<ConditionPayload> conditions
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TablePayload(
			String name, String label, String primaryColumn, List<ColumnPayload> columns
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ColumnPayload(
			String id, String name, String label, String type, String secondaryId
	) {
	}

}
