package com.bakdata.conquery.quarkus.storage;

import java.util.List;
import java.util.Optional;

public interface DatasetCatalogRepository {

	List<DatasetRecord> listDatasets();

	Optional<DatasetRecord> findDataset(String datasetId);

	void saveDataset(DatasetRecord dataset);

	boolean deleteDataset(String datasetId);

	List<ConceptRecord> listConcepts();

	List<ConceptRecord> listConceptsForDataset(String datasetId);

	Optional<ConceptRecord> findConcept(String conceptId);

	void saveConcept(ConceptRecord concept);

	boolean deleteConcept(String conceptId);

	List<TableRecord> listTables();

	List<TableRecord> listTablesForDataset(String datasetId);

	Optional<TableRecord> findTable(String tableId);

	void saveTable(TableRecord table);

	boolean deleteTable(String tableId);

	record DatasetRecord(
			String id,
			String label
	) {
	}

	record ConceptRecord(
			String id,
			String label
	) {
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
