package com.bakdata.conquery.quarkus.storage;

import java.util.*;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;

public interface DatasetCatalogRepository {

	List<DatasetRecord> listDatasets();

	Optional<DatasetRecord> findDataset(DatasetId datasetId);

	void saveDataset(DatasetRecord dataset);

	boolean deleteDataset(DatasetId datasetId);

	List<Concept> listConceptsForDataset(DatasetId datasetId);

	Optional<Concept> findConcept(ConceptId conceptId);

	void saveConcept(Concept concept);

	boolean deleteConcept(ConceptId conceptId);

	List<TableRecord> listTablesForDataset(DatasetId datasetId);

	Optional<TableRecord> findTable(TableId tableId);

	void saveTable(TableRecord table);

	boolean deleteTable(TableId tableId);

	record DatasetRecord(
			DatasetId id,
			String label
	) {
	}

	record ConceptElement(
			ConceptId id,
			String label,
			String description,
			ConceptId parentId,
			List<ConceptId> children,
			ConceptCondition condition
	) {
		public ConceptElement(ConceptId id, String label, String description) {
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
			ConceptId id,
			String label,
			String description,
			// All Children flat lookup
			Map<ConceptId,ConceptElement> children,
			// Direct children
			List<ConceptId> childrenIds,
			List<Connector> connectors

	){}

	record Connector(
			ConnectorId id,
			TableId tableId,
			ColumnId columnId,
			String label,
			String name,
			List<Select> selects,
			List<Filter> filters,
			// Use internal rep directly as we won't need data mangling
			List<ValidityDate> validityDates,
			boolean isDefault
	){
		public DatasetId datasetId() {
			return id.datasetId();
		}
	}

	record ValidityDate(
			String column,
			String startColumn,
			String endColumn
	) {}

	interface Select{}
	interface Filter{
		String id();
		String label();
		String type();
		List<String> options();
	}

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
			TableId id,
			String label,
			List<ColumnRecord> columns,
			ColumnId primaryColumn
	) {
		public TableRecord {
			columns = columns == null ? List.of() : List.copyOf(columns);
		}
	}

	record ColumnRecord(
			ColumnId id,
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
