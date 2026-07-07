package com.bakdata.conquery.quarkus.storage.xodus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusDatasetCatalogRepository implements DatasetCatalogRepository {

	private static final String DATASET_STORE = "dataset";
	private static final String CONCEPT_STORE = "concepts";
	private static final String TABLE_STORE = "tables";
	private static final String DATASET_KEY = "dataset";

	@Inject
	XodusDatasetEnvironmentProvider datasetEnvironmentProvider;

	@Inject
	ObjectMapper objectMapper;

	@Override
	public List<DatasetRecord> listDatasets() {
		List<DatasetRecord> datasets = new ArrayList<>();
		for (DatasetId datasetId : datasetEnvironmentProvider.listDatasetIds()) {
			findDataset(datasetId).ifPresent(datasets::add);
		}
		return datasets;
	}

	@Override
	public Optional<DatasetRecord> findDataset(DatasetId datasetId) {
		Optional<Environment> environmentOpt = datasetEnvironmentProvider.findEnvironment(datasetId);
		if (environmentOpt.isEmpty()) {
			return Optional.empty();
		}
		Environment environment = environmentOpt.get();
		return environment.computeInReadonlyTransaction(tx -> {
			var datasetStore = environment.openStore(DATASET_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			var entry = datasetStore.get(tx, StringBinding.stringToEntry(DATASET_KEY));
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(entry), DatasetRecord.class));
		});
	}

	@Override
	public void saveDataset(DatasetRecord dataset) {
		Environment environment = datasetEnvironmentProvider.getOrCreateEnvironment(dataset.id());
		environment.executeInTransaction(tx -> {
			var datasetStore = environment.openStore(DATASET_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			datasetStore.put(tx, StringBinding.stringToEntry(DATASET_KEY), StringBinding.stringToEntry(serialize(dataset)));
		});
	}

	@Override
	public boolean deleteDataset(DatasetId datasetId) {
		boolean exists = findDataset(datasetId).isPresent();
		datasetEnvironmentProvider.removeEnvironment(datasetId);
		return exists;
	}

	@Override
	public List<Concept> listConceptsForDataset(DatasetId datasetId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(datasetId);
		if (environment.isEmpty()) {
			return List.of();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var conceptsStore = environment.get().openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return readAllConcepts(conceptsStore, tx);
		});
	}

	@Override
	public Optional<Concept> findConcept(ConceptId conceptId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(conceptId.datasetId());
		if (environment.isEmpty()) {
			return Optional.empty();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var conceptsStore = environment.get().openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			var entry = conceptsStore.get(tx, StringBinding.stringToEntry(conceptId.toString()));
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(entry), Concept.class));
		});
	}

	@Override
	public void saveConcept(Concept concept) {
		Environment environment = datasetEnvironmentProvider.getOrCreateEnvironment(concept.id().datasetId());
		environment.executeInTransaction(tx -> {
			var conceptsStore = environment.openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			conceptsStore.put(tx, StringBinding.stringToEntry(concept.id().toString()), StringBinding.stringToEntry(serialize(concept)));
		});
	}

	@Override
	public boolean deleteConcept(ConceptId conceptId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(conceptId.datasetId());
		if (environment.isEmpty()) {
			return false;
		}
		return environment.get().computeInTransaction(tx -> {
			var conceptsStore = environment.get().openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return conceptsStore.delete(tx, StringBinding.stringToEntry(conceptId.toString()));
		});
	}

	@Override
	public List<TableRecord> listTablesForDataset(DatasetId datasetId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(datasetId);
		if (environment.isEmpty()) {
			return List.of();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var tablesStore = environment.get().openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return readAllTables(tablesStore, tx);
		});
	}

	@Override
	public Optional<TableRecord> findTable(TableId tableId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(tableId.datasetId());
		if (environment.isEmpty()) {
			return Optional.empty();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var tablesStore = environment.get().openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			var entry = tablesStore.get(tx, StringBinding.stringToEntry(tableId.toString()));
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(entry), TableRecord.class));
		});
	}

	@Override
	public void saveTable(TableRecord table) {
		Environment environment = datasetEnvironmentProvider.getOrCreateEnvironment(table.id().datasetId());
		environment.executeInTransaction(tx -> {
			var tablesStore = environment.openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			tablesStore.put(tx, StringBinding.stringToEntry(table.id().toString()), StringBinding.stringToEntry(serialize(table)));
		});
	}

	@Override
	public boolean deleteTable(TableId tableId) {
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(tableId.datasetId());
		if (environment.isEmpty()) {
			return false;
		}
		return environment.get().computeInTransaction(tx -> {
			var tablesStore = environment.get().openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return tablesStore.delete(tx, StringBinding.stringToEntry(tableId.toString()));
		});
	}

	private List<Concept> readAllConcepts(jetbrains.exodus.env.Store conceptsStore, Transaction tx) {
		List<Concept> result = new ArrayList<>();
		try (Cursor cursor = conceptsStore.openCursor(tx)) {
			while (cursor.getNext()) {
				result.add(deserialize(StringBinding.entryToString(cursor.getValue()), Concept.class));
			}
		}
		return result;
	}

	private List<TableRecord> readAllTables(jetbrains.exodus.env.Store tablesStore, Transaction tx) {
		List<TableRecord> result = new ArrayList<>();
		try (Cursor cursor = tablesStore.openCursor(tx)) {
			while (cursor.getNext()) {
				result.add(deserialize(StringBinding.entryToString(cursor.getValue()), TableRecord.class));
			}
		}
		return result;
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to serialize catalog value for Xodus", e);
		}
	}

	private <T> T deserialize(String payload, Class<T> type) {
		try {
			return objectMapper.readValue(payload, type);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to deserialize catalog value from Xodus", e);
		}
	}
}
