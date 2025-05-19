package com.bakdata.conquery.io.storage;

import java.util.Collection;
import java.util.List;
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
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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
public abstract class NamespacedStorageImpl implements Injectable, NamespacedStorage {

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

	public void openStores(ObjectMapper objectMapper) {
		if (objectMapper != null) {
			injectInto(objectMapper);
		}

		dataset = storageFactory.createDatasetStore(pathName, objectMapper);
		secondaryIds = storageFactory.createSecondaryIdDescriptionStore(pathName, objectMapper);
		tables = storageFactory.createTableStore(pathName, objectMapper);
		imports = storageFactory.createImportStore(pathName, objectMapper);
		concepts = storageFactory.createConceptStore(pathName, objectMapper);
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
	public Stream<ImportId> getAllImports() {
		return imports.getAllKeys().map(ImportId.class::cast);
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

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespacedStorageProvider.class, this)
					 .add(NamespacedStorage.class, this);
	}

	@Override
	public Dataset getDataset() {
		Dataset dataset = this.dataset.get();
		return dataset;
	}


	@Override
	public Stream<Table> getTables() {
		return tables.getAllKeys().map(TableId.class::cast).map(this::getTable);
	}

	@Override
	public Table getTable(TableId tableId) {
		return tables.get(tableId);
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
	public Stream<SecondaryIdDescription> getSecondaryIds() {
		return secondaryIds.getAllKeys().map(SecondaryIdDescriptionId.class::cast).map(this::getSecondaryId);
	}

	@Override
	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return getSecondaryIdFromStorage(descriptionId);
	}

	private SecondaryIdDescription getSecondaryIdFromStorage(SecondaryIdDescriptionId descriptionId) {
		return secondaryIds.get(descriptionId);
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
	public Stream<Concept<?>> getAllConcepts() {
		return concepts.getAllKeys().map(ConceptId.class::cast).map(this::getConcept);
	}

	@Override
	public Concept<?> getConcept(ConceptId id) {
		return getConceptFromStorage(id);
	}

	private Concept<?> getConceptFromStorage(ConceptId id) {
		return concepts.get(id);
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


	@Override
	public Collection<DatasetId> getAllDatasetIds() {
		return List.of(dataset.get().getId());
	}
}
