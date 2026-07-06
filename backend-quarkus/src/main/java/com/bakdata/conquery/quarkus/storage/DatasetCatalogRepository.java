package com.bakdata.conquery.quarkus.storage;

import java.util.*;

public interface DatasetCatalogRepository {

	List<DatasetRecord> listDatasets();

	Optional<DatasetRecord> findDataset(String datasetId);

	void saveDataset(DatasetRecord dataset);

	boolean deleteDataset(String datasetId);

	List<Concept> listConceptsForDataset(String datasetId);

	Optional<Concept> findConcept(String conceptId);

	void saveConcept(Concept concept);

	boolean deleteConcept(String conceptId);

	List<TableRecord> listTablesForDataset(String datasetId);

	Optional<TableRecord> findTable(String tableId);

	void saveTable(TableRecord table);

	boolean deleteTable(String tableId);

	record DatasetRecord(
			String id,
			String label
	) {
	}

	record ConceptElement(
			String id,
			String label,
			String description,
			String parentId,
			List<String> children,
			ConceptCondition condition
	) {
		public ConceptElement(String id, String label, String description) {
			this(id, label, description,null, List.of(), null);
		}

		public ConceptElement {
			children = children == null ? List.of() : List.copyOf(children);
		}

		public boolean hasResolvableCodes() {
			return condition != null && !condition.connectorValues().isEmpty();
		}
	}

	record Concept(
			String id,
			String label,
			String description,
			// All Children flat lookup
			Map<String,ConceptElement> children,
			// Direct children
			List<String> childrenIds,
			List<Connector> connectors

	){}

	record Connector(
			String column,
			String label,
			String name,
			List<Select> selects,
			List<Filter> filters,
			// Use internal rep directly as we won't need data mangling
			List<ValidityDate> validityDates,
			boolean isDefault
	){}

	record ValidityDate(
			String column,
			String startColumn,
			String endColumn
	) {}

	interface Select{}
	interface Filter{}

	record ConceptCondition(
			String type,
			List<String> values,
			String column,
			List<ConceptCondition> conditions
	) {
		public ConceptCondition {
			type = type == null ? null : type.trim().toUpperCase(Locale.ROOT);
			values = values == null ? List.of() : List.copyOf(values);
			conditions = conditions == null ? List.of() : List.copyOf(conditions);
		}

		public List<String> connectorValues() {
			// TODO This method is wrong, conditions don't work that way, that you can simply collect a final list of values from them
			return switch (type == null ? "" : type) {
				case "EQUAL" -> values;
				case "AND" -> conditions.stream()
						.flatMap(condition -> condition.connectorValues().stream())
						.toList();
				default -> List.of();
			};
		}
	}

	record TableRecord(
			String id,
			String label,
			List<ColumnRecord> columns,
			String primaryColumn
	) {
		public TableRecord {
			columns = columns == null ? List.of() : List.copyOf(columns);
		}
	}

	record ColumnRecord(
			String id,
			String label,
			ColumnType type,
			String secondaryId
	) {
	}

	enum ColumnType {
		STRING,
		INTEGER,
		BOOLEAN,
		REAL,
		DECIMAL,
		MONEY,
		DATE,
		DATE_RANGE
	}
}
