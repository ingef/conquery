package com.bakdata.conquery.quarkus.storage;

import java.util.*;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.SelectId;
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

	record Select(
			SelectId id,
			String label,
			String description,
			boolean defaultSelected,
			String implementationType,
			SelectResultType resultType,
			List<ColumnId> requiredColumns
	) {
		public Select {
			requiredColumns = requiredColumns == null ? List.of() : List.copyOf(requiredColumns);
		}
	}

	record SelectResultType(String type, SelectResultType elementType) {
		public static SelectResultType primitive(String type) {
			return new SelectResultType(type, null);
		}

		public static SelectResultType list(SelectResultType elementType) {
			return new SelectResultType("LIST", elementType);
		}
	}
	record Filter(
			FilterId id,
			String label,
			String type,
			String unit,
			String tooltip,
			List<FrontendValue> options,
			Integer min,
			Integer max,
			String pattern,
			boolean allowDropFile,
			boolean creatable,
			Object defaultValue,
			List<ColumnId> requiredColumns
	) {
		public Filter {
			options = options == null ? List.of() : List.copyOf(options);
			requiredColumns = requiredColumns == null ? List.of() : List.copyOf(requiredColumns);
		}
	}

	record FrontendValue(
			String value,
			String label,
			String optionValue
	) {
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
