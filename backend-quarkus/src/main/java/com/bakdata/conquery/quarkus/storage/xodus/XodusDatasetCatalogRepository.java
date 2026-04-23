package com.bakdata.conquery.quarkus.storage.xodus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.util.ScopedId;
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
		for (String datasetId : datasetEnvironmentProvider.listDatasetIds()) {
			findDataset(datasetId).ifPresent(datasets::add);
		}
		return datasets;
	}

	@Override
	public Optional<DatasetRecord> findDataset(String datasetId) {
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
	public boolean deleteDataset(String datasetId) {
		boolean exists = findDataset(datasetId).isPresent();
		datasetEnvironmentProvider.removeEnvironment(datasetId);
		return exists;
	}

	@Override
	public List<ConceptRecord> listConcepts() {
		List<ConceptRecord> concepts = new ArrayList<>();
		for (String datasetId : datasetEnvironmentProvider.listDatasetIds()) {
			concepts.addAll(listConceptsForDataset(datasetId));
		}
		return concepts;
	}

	@Override
	public List<ConceptRecord> listConceptsForDataset(String datasetId) {
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
	public Optional<ConceptRecord> findConcept(String conceptId) {
		Optional<String> derivedDatasetId = ScopedId.extractDatasetId(conceptId);
		if (derivedDatasetId.isEmpty()) {
			return Optional.empty();
		}
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(derivedDatasetId.get());
		if (environment.isEmpty()) {
			return Optional.empty();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var conceptsStore = environment.get().openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			var entry = conceptsStore.get(tx, StringBinding.stringToEntry(conceptId));
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(entry), ConceptRecord.class));
		});
	}

	@Override
	public void saveConcept(ConceptRecord concept) {
		String datasetId = ScopedId.extractDatasetId(concept.id())
				.orElseThrow(() -> new IllegalArgumentException("Concept id is not scoped by dataset: " + concept.id()));
		Environment environment = datasetEnvironmentProvider.getOrCreateEnvironment(datasetId);
		environment.executeInTransaction(tx -> {
			var conceptsStore = environment.openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			conceptsStore.put(tx, StringBinding.stringToEntry(concept.id()), StringBinding.stringToEntry(serialize(concept)));
		});
	}

	@Override
	public boolean deleteConcept(String conceptId) {
		Optional<String> derivedDatasetId = ScopedId.extractDatasetId(conceptId);
		if (derivedDatasetId.isEmpty()) {
			return false;
		}
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(derivedDatasetId.get());
		if (environment.isEmpty()) {
			return false;
		}
		return environment.get().computeInTransaction(tx -> {
			var conceptsStore = environment.get().openStore(CONCEPT_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return conceptsStore.delete(tx, StringBinding.stringToEntry(conceptId));
		});
	}

	@Override
	public List<TableRecord> listTables() {
		List<TableRecord> tables = new ArrayList<>();
		for (String datasetId : datasetEnvironmentProvider.listDatasetIds()) {
			tables.addAll(listTablesForDataset(datasetId));
		}
		return tables;
	}

	@Override
	public List<TableRecord> listTablesForDataset(String datasetId) {
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
	public Optional<TableRecord> findTable(String tableId) {
		Optional<String> derivedDatasetId = ScopedId.extractDatasetId(tableId);
		if (derivedDatasetId.isEmpty()) {
			return Optional.empty();
		}
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(derivedDatasetId.get());
		if (environment.isEmpty()) {
			return Optional.empty();
		}
		return environment.get().computeInReadonlyTransaction(tx -> {
			var tablesStore = environment.get().openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			var entry = tablesStore.get(tx, StringBinding.stringToEntry(tableId));
			if (entry == null) {
				return Optional.empty();
			}
			return Optional.of(deserialize(StringBinding.entryToString(entry), TableRecord.class));
		});
	}

	@Override
	public void saveTable(TableRecord table) {
		String datasetId = ScopedId.extractDatasetId(table.id())
				.orElseThrow(() -> new IllegalArgumentException("Table id is not scoped by dataset: " + table.id()));
		Environment environment = datasetEnvironmentProvider.getOrCreateEnvironment(datasetId);
		environment.executeInTransaction(tx -> {
			var tablesStore = environment.openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			tablesStore.put(tx, StringBinding.stringToEntry(table.id()), StringBinding.stringToEntry(serialize(table)));
		});
	}

	@Override
	public boolean deleteTable(String tableId) {
		Optional<String> derivedDatasetId = ScopedId.extractDatasetId(tableId);
		if (derivedDatasetId.isEmpty()) {
			return false;
		}
		Optional<Environment> environment = datasetEnvironmentProvider.findEnvironment(derivedDatasetId.get());
		if (environment.isEmpty()) {
			return false;
		}
		return environment.get().computeInTransaction(tx -> {
			var tablesStore = environment.get().openStore(TABLE_STORE, StoreConfig.WITHOUT_DUPLICATES, tx);
			return tablesStore.delete(tx, StringBinding.stringToEntry(tableId));
		});
	}

	private List<ConceptRecord> readAllConcepts(jetbrains.exodus.env.Store conceptsStore, Transaction tx) {
		List<ConceptRecord> result = new ArrayList<>();
		try (Cursor cursor = conceptsStore.openCursor(tx)) {
			while (cursor.getNext()) {
				result.add(deserialize(StringBinding.entryToString(cursor.getValue()), ConceptRecord.class));
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
