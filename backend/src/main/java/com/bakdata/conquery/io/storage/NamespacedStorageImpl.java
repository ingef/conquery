package com.bakdata.conquery.io.storage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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
public abstract class NamespacedStorageImpl implements NamespacedStorage {

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
	protected Store<String, Integer> entity2Bucket;


	public NamespacedStorageImpl(StoreFactory storageFactory, String pathName) {
		this.pathName = pathName;
		this.storageFactory = storageFactory;
	}

	@Override
	public ImmutableList<ManagedStore> getStores() {
		return ImmutableList.of(dataset, secondaryIds, tables, imports, concepts, entity2Bucket);
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
		entity2Bucket = storageFactory.createEntity2BucketStore(pathName, objectMapper);
	}



	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespacedStorage.class, this)
					 .add(NamespacedStorageProvider.class,this);
	}

	@Override
	public NamespacedStorage getStorage(@Nullable DatasetId datasetId) {
		DatasetId thisDatasetId = getDataset().getId();
		if (datasetId != null && !datasetId.equals(thisDatasetId)) {
			throw new IllegalArgumentException("Dataset id mismatch: expected " + thisDatasetId + " but got " + datasetId);
		}
		return this;
	}

	@Override
	public Collection<DatasetId> getAllDatasetIds() {
		return List.of(getDataset().getId());
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
	public Dataset getDataset() {
		return this.dataset.get();
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
	public int getEntityBucket(String entity) {
		return entity2Bucket.get(entity);
	}

	@Override
	public void addEntityToBucket(String entity, int bucket) {
		entity2Bucket.add(entity, bucket);
	}

	@Override
	public boolean hasEntity(String entity) {
		return entity2Bucket.hasKey(entity);
	}
}
