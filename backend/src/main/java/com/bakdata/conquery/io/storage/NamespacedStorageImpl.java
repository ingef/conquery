package com.bakdata.conquery.io.storage;

import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Overlapping storage structure for {@link WorkerStorageImpl} and {@link NamespaceStorage}.
 * The reason for the overlap ist primarily that all this stored members are necessary in the
 * SerDes communication between the manager and the shards/worker for the resolving of ids included in
 * messages.
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public abstract class NamespacedStorageImpl extends ConqueryStorage implements Injectable, NamespacedStorage {

	@Getter
	@ToString.Include
	private final String pathName;
	@Getter
	private final StoreFactory storageFactory;

	protected SingletonStore<Dataset> dataset;
	protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
	protected IdentifiableStore<Table> tables;
	protected IdentifiableStore<Import> imports;
	protected IdentifiableStore<Concept<?>> concepts;

	public NamespacedStorageImpl(StoreFactory storageFactory, String pathName) {
		this.pathName = pathName;
		this.storageFactory = storageFactory;
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(dataset, secondaryIds, tables, imports, concepts);
	}

	public void openStores(ObjectMapper objectMapper, MetricRegistry metricRegistry) {
		if (objectMapper != null) {
			injectInto(objectMapper);
		}

		dataset = storageFactory.createDatasetStore(pathName, objectMapper);
		secondaryIds = storageFactory.createSecondaryIdDescriptionStore(pathName, objectMapper);
		tables = storageFactory.createTableStore(pathName, objectMapper);
		imports = storageFactory.createImportStore(pathName, objectMapper);
		concepts = storageFactory.createConceptStore(pathName, objectMapper);

		decorateDatasetStore(dataset);
		decorateSecondaryIdDescriptionStore(secondaryIds);
		decorateTableStore(tables);
		decorateImportStore(imports);
		decorateConceptStore(concepts);
	}

	private void decorateDatasetStore(SingletonStore<Dataset> store) {
	}

	private void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
		// Nothing to decorate
	}

	private void decorateTableStore(IdentifiableStore<Table> store) {

	}

	private void decorateImportStore(IdentifiableStore<Import> store) {
		// Intentionally left blank
	}

	private void decorateConceptStore(IdentifiableStore<Concept<?>> store) {
		store.onAdd(concept -> {

			if (concept.getDataset() != null && !concept.getDataset().equals(dataset.get().getId())) {
				throw new IllegalStateException("Concept is not for this dataset.");
			}

			concept.setDataset(dataset.get().getId());

		});
	}

	// Imports

	@Override
	public void addImport(Import imp) {
		imports.add(imp);
	}

	@Override
	public Import getImport(ImportId id) {
		return getImportFromStorage(id);
	}

	private Import getImportFromStorage(ImportId id) {
		return imports.get(id);
	}

	@Override
	public Stream<Import> getAllImports() {
		return imports.getAll();
	}

	@Override
	public void updateImport(Import imp) {
		imports.update(imp);
	}

	@Override
	public void removeImport(ImportId id) {
		imports.remove(id);
	}

	// Datasets

	@Override
	public void updateDataset(Dataset dataset) {
		this.dataset.update(dataset);
	}
	public <ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id) {
		return (VALUE) id.get(this);
	}
@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespacedStorageProvider.class, this).
					 add(NamespacedStorage.class, this);
	}@Override
	public Dataset getDataset() {
		return dataset.get();
	}



	// Tables

		@Override
	public Table getTable(TableId tableId) {
		return getTableFromStorage(tableId);
	}

	private Table getTableFromStorage(TableId tableId) {
		return tables.get(tableId);
	}

	@Override
	public Stream<Table> getTables() {
		return tables.getAllKeys().map(TableId.class::cast).map(this::getTable);
	}


	@Override
	public void addTable(Table table) {
		tables.add(table);
	}

	@Override
	public void removeTable(TableId table) {
		tables.remove(table);
	}

	// SecondaryId

	@Override
	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return getSecondaryIdFromStorage(descriptionId);
	}

	private SecondaryIdDescription getSecondaryIdFromStorage(SecondaryIdDescriptionId descriptionId) {
		return secondaryIds.get(descriptionId);
	}

	@Override
	public Stream<SecondaryIdDescription> getSecondaryIds() {
		return secondaryIds.getAllKeys().map(SecondaryIdDescriptionId.class::cast).map(this::getSecondaryId);
	}

	@Override
	public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
		secondaryIds.add(secondaryIdDescription);
	}

	@Override
	public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
		secondaryIds.remove(secondaryIdDescriptionId);
	}

	// Concepts

	@Override
	public Concept<?> getConcept(ConceptId id) {
		return getConceptFromStorage(id);
	}

	private Concept<?> getConceptFromStorage(ConceptId id) {
		return concepts.get(id);
	}

	@Override
	public Stream<Concept<?>> getAllConcepts() {
		return concepts.getAllKeys().map(ConceptId.class::cast).map(this::getConcept);
	}

	@Override
	public boolean hasConcept(ConceptId id) {
		return concepts.get(id) != null;
	}

	@Override
	@SneakyThrows
	public void updateConcept(Concept<?> concept) {
		log.debug("Updating Concept[{}]", concept.getId());
		concepts.update(concept);
	}

	@Override
	public void removeConcept(ConceptId id) {
		log.debug("Removing Concept[{}]", id);
		concepts.remove(id);
	}

	// Utility




}
