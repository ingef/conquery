package com.bakdata.conquery.quarkus.storage;

import java.util.*;

import com.bakdata.conquery.quarkus.concepts.conditions.ConceptCondition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.FilterId;
import com.bakdata.conquery.quarkus.ids.SelectId;
import com.bakdata.conquery.quarkus.ids.StructureNodeId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.ids.ValidityDateId;

public interface DatasetCatalogRepository {

	List<DatasetRecord> listDatasets();

	Optional<DatasetRecord> findDataset(DatasetId datasetId);

	void saveDataset(DatasetRecord dataset);

	boolean deleteDataset(DatasetId datasetId);

	List<Concept> listConceptsForDataset(DatasetId datasetId);

	Optional<Concept> findConcept(ConceptId conceptId);

	void saveConcept(Concept concept);

	boolean deleteConcept(ConceptId conceptId);

	List<StructureNode> listStructureNodesForDataset(DatasetId datasetId);

	Optional<StructureNode> findStructureNode(StructureNodeId structureNodeId);

	void saveStructureNode(StructureNode structureNode);

	boolean deleteStructureNode(StructureNodeId structureNodeId);

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
			List<AdditionalInfo> additionalInfos,
			ConceptId parentId,
			List<ConceptId> children,
			ConceptCondition condition
	) {
		public ConceptElement(ConceptId id, String label, String description) {
			this(id, label, description, List.of(), null, List.of(), null);
		}

		public ConceptElement {
			additionalInfos = additionalInfos == null ? List.of() : List.copyOf(additionalInfos);
			children = children == null ? List.of() : List.copyOf(children);
		}

	}

	record Concept(
			ConceptId id,
			String label,
			String description,
			List<AdditionalInfo> additionalInfos,
			boolean defaultExcludeFromTimeAggregation,
			// All Children flat lookup
			Map<ConceptId,ConceptElement> children,
			// Direct children
			List<ConceptId> childrenIds,
			List<Connector> connectors

	){
		public Concept {
			additionalInfos = additionalInfos == null ? List.of() : List.copyOf(additionalInfos);
		}

		// TODO Add concept-level selects (for example EXISTS) once their separate provider and ID hierarchy is migrated.
	}

	record AdditionalInfo(String key, String value) {
	}

	record StructureNode(
			StructureNodeId id,
			String label,
			String description,
			int sourceOrder,
			StructureNodeId parentId,
			List<StructureNodeId> children,
			List<ConceptId> containedRoots
	) {
		public StructureNode {
			children = children == null ? List.of() : List.copyOf(children);
			containedRoots = containedRoots == null ? List.of() : List.copyOf(containedRoots);
		}

		// TODO Add structure-node additionalInfos once the shared frontend metadata model is migrated.
	}

	record Connector(
			ConnectorId id,
			TableId tableId,
			ColumnId columnId,
			String label,
			String name,
			List<Select> selects,
			List<Filter> filters,
			String validityDatesDescription,
			List<ValidityDate> validityDates,
			boolean isDefault
	){
		public DatasetId datasetId() {
			return id.datasetId();
		}
	}

	record ValidityDate(
			ValidityDateId id,
			String label,
			ColumnId columnId,
			ColumnId startColumnId,
			ColumnId endColumnId
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
